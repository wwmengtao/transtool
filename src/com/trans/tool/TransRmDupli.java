package com.trans.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**TransDom4jRmDupli：
 * 此类用于消除文件中的重复Element节点*/
public class TransRmDupli extends TransCommon{
	String DirToRemoveDuplicate=null;
	ArrayList<String> listofSrcXmlFiles=new ArrayList<String>();
	//isBeginElementRemains用于表明在消除重复字串过程中保留第一个还是最后一个
	static boolean isBeginElementRemains=false;
	public static void main(String []args){
		if(isInConsoleEnviroment){
	    	if (args.length != 2) {
	    		dayin("Please input correct command under Linux environment ! \nsuch as: \"java -cp *.jar com.trans.tool.TransRmDupli filenameFilter.xml false\"");
	            return;
	        }
	    	isBeginElementRemains=args[1].equals("true")?true:false;
		}else{
			isBeginElementRemains = false;
		}
		if(!new File(fileName_filter).exists()){
			dayin("Please make sure \""+fileName_filter+"\" exists!");
			return;
		}
		new TransRmDupli();
	}
	
	public TransRmDupli(){
		try{
			Document doc= saxread.read(new File(fileName_filter));// 读取XML文件
			List<Element> list_mergeDir=doc.selectNodes("/filename/mergeDir");	
			DirToRemoveDuplicate=list_mergeDir.get(0).getData().toString().trim();
			if(!isDirProper(DirToRemoveDuplicate,"DirToRemoveDuplicate")){return;}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		getSrcXmlFiles(new File(DirToRemoveDuplicate),new myFilenameFilter());
		for(int i=0;i<listofSrcXmlFiles.size();i++)	{
			if(isBeginElementRemains){
				RemoveDuplicateBeginRemain(listofSrcXmlFiles.get(i));
			}else{
				RemoveDuplicateEndRemain(listofSrcXmlFiles.get(i));
			}
		}
	}

	/**RemoveDuplicateBeginRemain：
	 * 此函数仅保留文件中第一个Element节点*/
	public void RemoveDuplicateBeginRemain(String filename){
		Document doc=null;
		boolean isFileChanged=false;
		try{
			doc= saxread.read(new File(filename));// 读取XML文件
		}catch(DocumentException e){
			e.printStackTrace();
		}
		Element eleRoot_target=(Element)doc.getRootElement();
		Element ele_target=null,ele_temp=null;
		@SuppressWarnings("rawtypes")
		List list_target=eleRoot_target.elements();
		int count=0;
		while(list_target.size()-count>=2){
			ele_target=(Element)list_target.get(count);
			for(int i=list_target.size()-1;i>count;i--){
				ele_temp= (Element)list_target.get(i);
				if(isElementsAttrEqual(ele_target,ele_temp)){
					addAttibute(ele_target,ele_temp);
					list_target.remove(i);
					isFileChanged=true;
				}
			}
			count++;
		}
		if(isFileChanged&&doc.getRootElement().elements().size()>0){
			writeOutFile(doc,new File(filename));
		}
	}
	
	public void addAttibute(Element ele_target,Element ele_temp){//此函数用于修改ele_target的"translatable"属性数值
		String strTranslatable_target,strTranslatable_temp;
		strTranslatable_target=ele_target.attributeValue("translatable");
		if(null==strTranslatable_target||strTranslatable_target.equals("true")){
			strTranslatable_temp=ele_temp.attributeValue("translatable");
			if(null!=strTranslatable_temp&&strTranslatable_temp.equals("false")){
				ele_target.addAttribute("translatable", "false");
			}
			
		}
	}
	
	/**RemoveDuplicateEndRemain：
	 * 此函数仅保留文件中最后一个Element节点*/
	public void RemoveDuplicateEndRemain(String filename){
		Document doc=null;
		boolean isFileChanged=false;
		try{
			doc= saxread.read(new File(filename));// 读取XML文件
		}catch(DocumentException e){
			e.printStackTrace();
		}
		Element eleRoot_target=(Element)doc.getRootElement();
		Element ele_target=null,ele_temp=null;
		@SuppressWarnings("rawtypes")
		List list_target=eleRoot_target.elements();
		int count=0;
		while(list_target.size()-count>=2){
			int index=list_target.size()-1-count;
			ele_target=(Element)list_target.get(index);
			for(int i=0;i<index;i++){
				ele_temp= (Element)list_target.get(i);
				if(isElementsAttrEqual(ele_target, (Element)list_target.get(i))){
					addAttibute(ele_target,ele_temp);
					isFileChanged=true;
					list_target.remove(i);
					i--;
					index--;
				}
			}
			count++;
		}

		if(isFileChanged&&doc.getRootElement().elements().size()>0){
			writeOutFile(doc,new File(filename));
		}
	}
	
	/**
	* 使用的是FilenameFilter过滤器。 
	*/ 
	public void getSrcXmlFiles(File srcDir, myFilenameFilter filter) { 
		File[] files = srcDir.listFiles(); 
		for (File file : files) {
			if(!file.isDirectory()){
				if(filter.accept(file, file.getName())){
					listofSrcXmlFiles.add(file.getAbsolutePath());
				}
			}else{
				getSrcXmlFiles(file, filter); 
			}
		}//for
	} 
	/**
	 * 如下为文件名过滤器的使用
	 * */
	public class myFilenameFilter  implements FilenameFilter{
		public boolean isXmlFile(String filename){
			if(filename.endsWith(".xml")){
				return true;
			}
			return false;
		 }
		public boolean accept(File dir, String filename) {
			return isXmlFile(filename);
		}
	}	
}
