package com.trans.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class TransCommon {
	static boolean isInConsoleEnviroment=true;
	static String fileName_filter 		="filenameFilter.xml";	
	//以下文件/目录名称为调试状态下使用
	static String fileName_miss		="ConfigeFiles\\Dom4j\\missing_translation.xml";
	static String fileName_reuse		="ConfigeFiles\\Dom4j\\reuse_translation.xml";
	static String fileName_import 	="ConfigeFiles\\Dom4j\\import_translation.xml";
	static String fileName_insert 		="ConfigeFiles\\Dom4j\\insertTrans.xml";
	String outFileFormat="\n";
	String FORMAT_RESOURCE="resources";
	String Namespace1="xliff",Namespace2="urn:oasis:names:tc:xliff:document:1.2";
	boolean isNeedRefreshOutDir=true;
	protected SAXReader saxread=null;//定义文件读取器并定义读取文件的格式为utf-8
	{
		saxread = new SAXReader();
		saxread.setEncoding("utf-8");
	}
	
	//isArrayElementEqual：决定在查找缺失时，参照文件中是否需要删除该数组Element节点
	int isArrayElementEqual=-1;
	public static File getConfigFile(String []args,String FileName,boolean isInConsoleEnviroment){
		File configFile=null;
		String conFileName=null;
		if(isInConsoleEnviroment){
	    	if (args.length != 1) {
	    		dayin("Please input correct command under Linux environment ! \nsuch as: \"java -cp *.jar com.trans.tool.Trans** *.xml\"");
	            return null;
	        }
	    	conFileName=args[0];
		}else{
			conFileName = FileName;
		}
		if(!new File(fileName_filter).exists()){
			dayin("Please make sure \""+fileName_filter+"\" exists!");
			return null;
		}
		configFile=new File(conFileName);
		if (!configFile.exists()) {
			dayin("\""+conFileName+"\" not exist!");
			return null;
		}
		return configFile;
	}

	public boolean isDirProper(String Dir,String dirName){
		if(!new File(Dir).exists()||!new File(Dir).isDirectory()){
			dayin(dirName+":\""+Dir+"\" does not exists or is not a directory!");
			return false;
		}
		if(Dir.endsWith(File.separator)){
			dayin(dirName+":\""+Dir+"\" can not ends with "+File.separator);
			return false;
		}
		return true;
	}
	
	/**下列进行文件的整体拷贝*/
	public void selectToCreateFile(String baseFileName,String outFileName){
			Element ele_src;
			Document doc_srcFile=null,doc_outFile=null;
			List list_srcFile;
    		doc_outFile=DocumentHelper.createDocument();//创建根节点
    		Element rootOutFile=doc_outFile.addElement(FORMAT_RESOURCE);
    		rootOutFile.addNamespace(Namespace1,Namespace2);
    		rootOutFile.addText(outFileFormat);
    		//dayin(root_temp.getXPathResult(0).getText());//数值为：urn:oasis:names:tc:xliff:document:1.2
     		//dayin(root_temp.getNamespaceForPrefix("xliff").asXML());//数值为：xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
    		try{
    			doc_srcFile = saxread.read(new File(baseFileName));
    		}catch(Exception e){
    			e.printStackTrace();
    		}    			
    		list_srcFile=((Element)doc_srcFile.getRootElement()).elements();
    		for(int i=0;i<list_srcFile.size();i++){
    			ele_src=(Element)list_srcFile.get(i);
    			ele_src.setParent(null);
    			rootOutFile.add(ele_src);
    			rootOutFile.addText(outFileFormat);
    		}
    		if(doc_outFile.getRootElement().elements().size()>0){//最终的缺失文件必须包含至少一个有效Element类型节点
    			writeOutFile(doc_outFile,new File(outFileName));
    		}
	}	
	
	public ArrayList<Element> gatherNodesProper(File file,boolean isFindMissingBaseFile){
		ArrayList<Element> list_File=new ArrayList<Element>();
		Node node=null;
		Document doc=null;
		int index=0;
		try{
			doc= saxread.read(file);// 读取XML文件
		}catch(Exception ex){
			ex.printStackTrace();
		}
        @SuppressWarnings("rawtypes")
		List lt_File = doc.getRootElement().content();  
        @SuppressWarnings("rawtypes")
		Iterator iterator = lt_File.iterator();  
        Element ele_temp;
        while(iterator.hasNext()){//while循环可首先消除下列格式节点：
        	/**
        	 <!-- Do not translate. Used as the value for a setting. -->
    		<string name="default_date_format">xxx</string>	
        	 */
        	node = (Node)iterator.next(); 
        	if(Node.COMMENT_NODE==node.getNodeType()){
        		if(node.getText().toLowerCase().contains("Do not translate".toLowerCase())){
        			index=1;
        		}
        	}
        	if(Node.ELEMENT_NODE==node.getNodeType()){
        		if(1==index){//说明此节点不需要翻译
	        		index=0;
        		}else{//此时可以做一些节点比对工作
        			ele_temp=(Element)node;
        	    	if(isElementFormatOK(ele_temp)){//判断形式是否符合要求
        	    		//如果当前正在查找翻译缺失，并且当前文件是基准文件，则还需要判断元素节点是否需要翻译
        	    		boolean isNeedTranslated=(isFindMissingBaseFile)?isNeedTranslate(ele_temp):true;
        	    		if(isNeedTranslated){
	        	    		ele_temp.setParent(null);
	        	    		list_File.add(ele_temp);
        	    		}
        	    	}//if
        		}//if
        	}//if
        }//while
        return list_File;
	}
	
	public void writeOutFile(Document doc_outFile,File outFile){
		try{
				if(!outFile.getParentFile().exists()){
					outFile.getParentFile().mkdirs();
				}
				/** 将document中的内容写入文件中 */
				/*
				java.io.OutputStream out=new java.io.FileOutputStream(outFile);
				java.io.Writer wr=new java.io.OutputStreamWriter(out,"utf-8");
				doc_outFile.write(wr);
				wr.close();
				out.close();	
				
				OutputFormat of=new OutputFormat("\t",true);//用于控制节点的输出格式
				of.setEncoding("utf-8");
				of.setXHTML(true);
				XMLWriter writer = new XMLWriter(new FileWriter(outFile),of);
				writer.write(doc_outFile);
				writer.close();

				
				XMLWriter writer = new XMLWriter(new FileWriter(outFile));
				writer.write(doc_outFile);
				writer.close();
				*/
				OutputFormat of=new OutputFormat("",false);//用于控制节点的输出格式
				of.setEncoding("utf-8");
				XMLWriter writer = new XMLWriter(new FileWriter(outFile),of);
				writer.write(doc_outFile);
				writer.close();
	    }catch(Exception ex){
	    	ex.printStackTrace();
	    }
	}	
	
	public void refreshOutDir(String outdir){//目录存在，则删除后重建；否则创建目录。
		String outputDir=outdir+File.separator;
		//下列if语句保证输出结果的目录存在
		if(!new File(outputDir).exists()){
			new File(outputDir).mkdirs();
		}else{
			deleteDir(new File(outputDir));
			new File(outputDir).mkdirs();
		}
	}
	
	/**删除指定文件夹下所有内容*/
	public void deleteDir(File file){
		if(!file.exists()){//文件夹不存在
			dayin("指定目录不存在:"+file.getName());
			return;
		}
		boolean rslt=true;//保存中间结果
		if(!(rslt=file.delete())){//先尝试直接删除
			//若文件夹非空。枚举、递归删除里面内容
			File subs[] = file.listFiles();
			for (File afile : subs) {
				if (afile.isDirectory()){
					deleteDir(afile);//递归删除子文件夹内容
				}
				rslt = afile.delete();//删除子文件夹本身
			}
			rslt = file.delete();//删除此文件夹本身
		}
		if(!rslt){
			dayin("无法删除:"+file.getName());
			return;
		}
	}

	@SuppressWarnings("unchecked")
	public void insertElements(List list_srcFile,Document doc_targetFile){
		int i;
		Element eleRoot_target=(Element)doc_targetFile.getRootElement();
		@SuppressWarnings("rawtypes")
		List list_target=eleRoot_target.elements();
		Element ele_src=null,ele_target=null;
		while(0!=list_srcFile.size()){
			ele_src=(Element)list_srcFile.get(0);
			list_srcFile.remove(ele_src);
			ele_src.setParent(null);
			for(i=0;i<list_target.size();i++){
				ele_target=(Element)list_target.get(i);
				if(isElementsFormEqual(ele_src,ele_target)){
					list_target.set(list_target.indexOf(ele_target),ele_src);
				    break;
				}
			}
			if(list_target.size()==i){//表明ele_src在doc_targetFile找不到各标识相同的元素节点，应该追加
				eleRoot_target.add(ele_src);
				eleRoot_target.addText(outFileFormat);	
			}
		}
	}	
	public boolean isFileNotALink(File filename){//Judge whether the file is a link
		String fileCanonicalPath=null;
		try {
			fileCanonicalPath=filename.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileCanonicalPath.equals(filename.getAbsolutePath());
	}
	/**
	 * 下列函数用于字符串复用：
	 * isPluralsElementsCanBeReused
	 * isStringArrayElementsCanBeReused
	 * IsArrayElementsCanBeReused
	 * */
	public boolean isPluralsElementsCanBeReused(Element ele_src,Element ele_target){

		@SuppressWarnings("rawtypes")
		List lt_src,lt_target;
		lt_src=ele_src.elements();
		lt_target=ele_target.elements();
		if(lt_src.size()!=lt_target.size()){
			return false;
		}
		boolean isTextDiffer=false;
		Element item_src=null,item_target=null;
		String attr_src=null,attr_target=null;
		for(int j=0;j<lt_src.size();j++){
			item_src=(Element)lt_src.get(j);
			attr_src=item_src.attribute("quantity").getValue();
			for(int i=0;i<lt_target.size();i++){
				item_target=(Element)lt_target.get(i);
				attr_target=item_target.attribute("quantity").getValue();
				if(attr_src.equals(attr_target)){
					break;
				}
			}
			if(!isStringFormEqual(item_src.getTextTrim(), item_target.getTextTrim())){
				isTextDiffer=true;
				break;
			}
		}
		return isTextDiffer;
	}
	
	public boolean isStringArrayElementsCanBeReused(Element ele_src,Element ele_target){
		Element item_src,item_target;
		@SuppressWarnings("rawtypes")
		List lt_src,lt_target;
		lt_src=ele_src.elements();
		lt_target=ele_target.elements();
		if(lt_src.size()!=lt_target.size()){
			return false;
		}
		for(int i=0;i<lt_src.size();i++){
			item_src=(Element)lt_src.get(i);
			item_target=(Element)lt_target.get(i);
			if(!isStringFormEqual(item_src.getTextTrim(),item_target.getTextTrim())){
				return true;
			}
		}		
		return false;
	}	
	
	public boolean isArrayElementsCanBeReused(Element ele_src,Element ele_target){
		String tagname=ele_src.getName();
		boolean blArrayElementsCanBeReused=false;
		if(tagname.equals("string-array")){
			blArrayElementsCanBeReused = isStringArrayElementsCanBeReused(ele_src,ele_target);
		}else if(tagname.equals("plurals")){
			blArrayElementsCanBeReused = isPluralsElementsCanBeReused(ele_src,ele_target);
		}
		return blArrayElementsCanBeReused;
	}	
	
	/**isStringFormEqual：
	 * 在节点元素复用功能中，需要判断<string/>以及数组类元素节点内容是否相同，
	 * 但是考虑到有可能因为首尾引号问题将"ABC"和ABC内容判为不相同的情况，
	 * 因此通过此函数去除待比较两个字符串首尾引号后再比较。*/
	public boolean isStringFormEqual(String str_src,String str_target){
		//以下去除待比较两个字符串的首尾引号，然后进行比较
		if(str_src.startsWith("\"")&&str_src.endsWith("\"")){
			str_src = str_src.substring(1,str_src.length()-1);
		}
		if(str_target.startsWith("\"")&&str_target.endsWith("\"")){
			str_target = str_target.substring(1,str_target.length()-1);
		}
		if(str_src.equals(str_target)){
			return true;
		}
		return false;
	}	

	/**isElementsAttrEqual：
	 * 判断两个节点属性值是否对应相同*/	
	public boolean isElementsAttrEqual(Element ele_src,Element ele_target){
		String tagname_src,attrname_src,attrproduct_src;
		String tagname_target,attrname_target,attrproduct_target;
		tagname_src=ele_src.getName();
		tagname_target=ele_target.getName();
		attrname_src=ele_src.attributeValue("name");
		attrname_target=ele_target.attributeValue("name");		
		attrproduct_src=ele_src.attributeValue("product");
		attrproduct_target=ele_target.attributeValue("product");	
		if(tagname_src.equals(tagname_target)&&isAttrValueEqual(attrname_src,attrname_target)&&isAttrProductEqual(attrproduct_src,attrproduct_target)){
			return true;
		}
		return false;
	}
	
	/**isElementsFormEqual：
	 * 判断两个节点属性值是否对应相同，以及数组节点item个数是否相同*/
	public boolean isElementsFormEqual(Element ele_src,Element ele_target){
		boolean blElementsFormEqual=false;
		String tagname_src=ele_src.getName();
		if(isElementsAttrEqual(ele_src,ele_target)){
			blElementsFormEqual=true;//先默认非数组类Element元素形式上相同，例如<string xxx>或者<skip>、<dimen>
			if(tagname_src.equals("string-array")||tagname_src.equals("plurals")){//如果是数组类Element则需做节点个数统计
				blElementsFormEqual=(ele_src.elements().size()==ele_target.elements().size());
				if(!blElementsFormEqual){
					isArrayElementEqual=0;
				}
			}
		}else{
			blElementsFormEqual=false;
		}
		return blElementsFormEqual;
	}		
	
	/**isStringFormatOK：
	 * 下列节点在使用节点的getText()时得到的是""，即空字符串
	 * <string name="sync_item_title"><xliff:g id="authority" example="Calendar">%s</xliff:g></string>*/
	public boolean isStringFormatOK(Element ele_node){
		boolean blStringFormatOK=false;
		String str=ele_node.getTextTrim();
		 if(str.startsWith("@string/")||str.equals("\"\"")){
			 return blStringFormatOK;
		 }else if(str.equals("")){
			 //dayin(ele_node.attributeValue("name"));
			/**<string name="app_name"><xliff:g id="data">Data:</xliff:g></string>
			 * <string name="app_name"><xliff:g id="data">%s</xliff:g><xliff:g id="data">%2$d</xliff:g></string>
			 * */
			 str=ele_node.getStringValue().trim().toLowerCase();
			 //dayin(str);
			 if(str.startsWith("%")){
				 switch(str.length()){
				 case 2://判断str是不是"%s"或者"%d"
					 blStringFormatOK=false;
					 break;
				 case 4://判断str是不是"%n$s"或者"%n$d"
					 blStringFormatOK=(!str.endsWith("$s")&&!str.endsWith("$d"));
					 break;
				 default:
					 blStringFormatOK=true;
				 }
				 return blStringFormatOK;
			 }
		 }
		 return true;
	}
	
	public boolean isElementFormatOK(Element ele_node){//元素节点必须符合要求，目前仅支持如下三种类型的元素节点
		String tagname=null,attrname_translatable=null;
		Element ele_temp=null;
		tagname=ele_node.getName();
		boolean blElementOK=true;
		if(tagname.equals("string")){
			/**注意：下列类型字符串执行ele_node.getTextTrim()后为空串，但是执行ele_node.getStringValue().trim()却非空
			 * <string name="service_client_name"><xliff:g id="client_name">%1$s</xliff:g></string>
			 * <string name="service_client_name1"><xliff:g id="client_name1">Data:%1$s</xliff:g></string>
			 * */
			blElementOK = isStringFormatOK(ele_node);
		}else if(tagname.equals("string-array")||tagname.equals("plurals")){
			int index=0;
			int count_item=0;
			Node node=null;
			@SuppressWarnings("rawtypes")
			List lt_File = ele_node.content(); 
			@SuppressWarnings("rawtypes")
			Iterator iterator = lt_File.iterator();
	        while(iterator.hasNext()){
	        	node = (Node)iterator.next(); 
	        	if(Node.COMMENT_NODE==node.getNodeType()){
	        		if(node.getText().toLowerCase().contains("Do not translate".toLowerCase())){
	        			index=1;
	        		}
	        	}else if(Node.ELEMENT_NODE==node.getNodeType()){
	        		ele_temp=(Element)node;
	        		attrname_translatable=ele_temp.attributeValue("translatable");
	        		if(1==index||!isStringFormatOK(ele_temp)||(null!=attrname_translatable&&attrname_translatable.equals("false"))){//说明此节点格式上不符合要求
		        		index=0;
		        		count_item++;
	        		}
	        	}//if
	        }//while
	        if(ele_node.elements().size()==count_item){
	        	blElementOK=false;
	        }
		}else{//如果是其他形式的元素节点，例如<integer name="asd">asdf</integer>，<skip/>则此节点不合要求
			blElementOK=false;
		}
		return blElementOK;
	}
		
	@SuppressWarnings("rawtypes")
	public boolean isContainExcludePath(String filename,List exclude_path){
		Iterator it_ep=exclude_path.iterator();
		Element ele_temp;
		while(it_ep.hasNext()){
			ele_temp = (Element)it_ep.next(); 
			if(filename.contains(ele_temp.getTextTrim())){
				return true;
			}
		}
		return false;
	}	
	
	/**下列判断是否需要翻译*/
	public boolean isNeedTranslate(Element ele){
		boolean IsNeedTranslate=false;
		String attr_translatable=ele.attributeValue("translatable");
		if(null==attr_translatable){
			IsNeedTranslate=true;
		}else{
			IsNeedTranslate=(ele.attributeValue("translatable").equals("true"))?true:false;
		}
		return IsNeedTranslate;
	}		
	
	public boolean isAttrValueEqual(String attr_product1,String attr_product2){
		boolean IsEqual=false;
		if(null==attr_product1){
			IsEqual=(null==attr_product2)?true:false;
		}
		else{
			IsEqual=(null==attr_product2)?false:attr_product2.equals(attr_product1);
		}
		return IsEqual;
	}		
	
	public boolean isAttrProductEqual(String attr_product1,String attr_product2){
		boolean IsEqual=false;
		if(null==attr_product1){
			IsEqual=(null==attr_product2||attr_product2.toLowerCase().equals("default"))?true:false;
		}
		else{
			IsEqual=(null==attr_product2)?attr_product1.toLowerCase().equals("default"):attr_product1.equals(attr_product2);
		}
		return IsEqual;
	}
	
	/**parseFilenameFilterFile：
	 * 此函数用于获取配置文件中的文件名*/
	public ArrayList<String> getInitialXmlFiles(){
		File filenameFilter=new File(fileName_filter);
		ArrayList<String> xmlFiles=new ArrayList<String>();
		try{
			Document doc= saxread.read(filenameFilter);// 读取XML文件
			//list_baseXmlFiles中存储的是基础文件(即其它文件可能并入其中)，例如strings.xml、arrays.xml
			List<Element> list_baseXmlFiles=doc.getRootElement().elements();
			List<Element> list_subXmlFiles;
			String str_temp;
			for(int i=0;i<list_baseXmlFiles.size();i++){
				str_temp=list_baseXmlFiles.get(i).getName();
				if(!str_temp.equals("mergeDir")){
					xmlFiles.add(str_temp);
				}
				list_subXmlFiles=doc.selectNodes("/filename/"+str_temp+"/file");
				addXmlFiles(list_subXmlFiles,xmlFiles);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return xmlFiles;
	}
	
	public void addXmlFiles(List<Element> list_configFile,ArrayList<String> xmlFiles){
		boolean isStringEqual=false;
		String STR=null;
		for(int i=0;i<list_configFile.size();i++){
			STR=list_configFile.get(i).getData().toString().trim();
			for(int k=0;k<xmlFiles.size();k++){
				if(xmlFiles.get(k).equals(STR)){
					isStringEqual=true;
					break;
				}
			}
			if(!isStringEqual&&!STR.equals("")){
				xmlFiles.add(STR);
			}
			isStringEqual=false;
		}

	}
	
	public void addXmlFiles(String STR,ArrayList<String> xmlFiles){
		boolean isStringEqual=false;
		for(int k=0;k<xmlFiles.size();k++){
			if(xmlFiles.get(k).equals(STR)){
				isStringEqual=true;
				break;
			}
		}
		if(!isStringEqual&&!STR.equals("")){
			xmlFiles.add(STR);
		}
	}
	
	public ArrayList<String> getStringTokenizer(String stringToBeConv){
		ArrayList<String> localeList = new ArrayList<String>();

	    if (stringToBeConv != null) {
	        StringTokenizer tokenizer = new StringTokenizer(stringToBeConv, ",");
	        while (tokenizer.hasMoreTokens()) {
	            localeList.add(tokenizer.nextToken().trim());
	        }
	    }
	    return localeList;
	}		
	
	public static void dayin(Object STR)
	{
		System.out.println(STR);
	}	
	
	/**
	 * 下列referHandler在当前代码中的使用方式如下：
		referHandler myreferHandler=new referHandler();
		saxread.addHandler("/resources/string", myreferHandler);
		saxread.addHandler("/resources/string-array", myreferHandler);
		doc_referFile = saxread.read(referFileName);
	 * */
	public class referHandler  implements ElementHandler{
		Element ele_handler;
	    public void onStart(ElementPath path) {
	    	
	    }
	    
	    public void onEnd(ElementPath path) {
	    	ele_handler = path.getCurrent();
	    	ele_handler.detach();
	    }
	}
		
}
