package com.trans.tool;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TransMiss extends TransCommon{

	ArrayList<String> listofBaseXmlDirs;
	ArrayList<String> listofLayoutFiles,listofSrcFiles,listofProjectDirs;
	String basedir,referdir,deletedir,outdir,baselocale;
	ArrayList<String> locales_list;
	@SuppressWarnings("rawtypes")
	List exclude_path;
	boolean isReferFileChanged=false;
	public static void main(String []args){
		File configFile = getConfigFile(args,fileName_miss,isInConsoleEnviroment);	
		if(null!=configFile){
			new TransMiss(configFile);
		}
	}
	public TransMiss(File configFile){
		ParseConfigFile(configFile);
	}
	@SuppressWarnings("unchecked")
	public void ParseConfigFile(File configFile){
		try{
			Document doc= saxread.read(configFile);// 读取XML文件
			List<Element> list_configFile;
			String locales;
			list_configFile=doc.selectNodes("/config/basedir");
			//dayin(list_configFile.size());
			basedir=list_configFile.get(0).getData().toString().trim();
			if(!isDirProper(basedir,"basedir")){return;}
			list_configFile=doc.selectNodes("/config/referdir");				
			referdir=list_configFile.get(0).getData().toString().trim();
			if(!referdir.equals("")&&!isDirProper(referdir,"referdir")){return;}
			list_configFile=doc.selectNodes("/config/deletedir");	
			deletedir=list_configFile.get(0).getData().toString().trim();
			if(!deletedir.equals("")&&!isDirProper(deletedir,"deletedir")){return;}
			list_configFile=doc.selectNodes("/config/outdir");	
			outdir=list_configFile.get(0).getData().toString().trim();
			//
			list_configFile=doc.selectNodes("/config/baselocale");
			baselocale=list_configFile.get(0).getData().toString().trim();
			list_configFile=doc.selectNodes("/config/locales");
			locales=list_configFile.get(0).getData().toString().trim();
			exclude_path=doc.selectNodes("/config/exclude-path");
		    locales_list = getStringTokenizer(locales);
		    //
		    if(0!=locales_list.size()){
				if(isNeedRefreshOutDir){
					refreshOutDir(outdir);
				}
				listofBaseXmlDirs=new ArrayList<String>();			    
			    findMissingTranslations();
		    }
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void findMissingTranslations(){
		listofProjectDirs=new ArrayList<String>();	//获取所有子工程根目录
		getBaseDirs(new File(basedir),new myFilenameFilter());
		ArrayList<String> filesToFindMiss=getInitialXmlFiles();
		String baseFileName,referFileName,outFileName;
		ArrayList<Element> list_srcFile=null;
		String preDirProject="";
		String dirProject=null;
		String BaseXmlDir=null;
		String str_androidmanifest=null;
		for(int k=0;k<listofBaseXmlDirs.size();k++){
			BaseXmlDir=listofBaseXmlDirs.get(k).replace(basedir, deletedir);
			dirProject=getProjectDir(BaseXmlDir,listofProjectDirs);
			if(null!=dirProject&&!dirProject.equals(preDirProject)){
				listofLayoutFiles=new ArrayList<String>();
				listofSrcFiles=new ArrayList<String>();
				getLayoutSrcFiles(new File(dirProject), new myFilenameFilter_LayoutSrc());
				str_androidmanifest=dirProject+File.separator+"AndroidManifest.xml";
				if(new File(str_androidmanifest).exists()){
					addXmlFiles(str_androidmanifest,listofLayoutFiles);
				}
				preDirProject=dirProject;
			}
			for(int j=0;j<filesToFindMiss.size();j++){
				baseFileName=listofBaseXmlDirs.get(k)+File.separator+filesToFindMiss.get(j);
				if(!new File(baseFileName).exists()){
					continue;
				}
				list_srcFile=gatherNodesProper(new File(baseFileName),true);
				if(null!=dirProject){//表明Android应用子工程存在
					filterElementsNotUsed(list_srcFile,listofLayoutFiles,listofSrcFiles);
				}
				for(int i=0;i<locales_list.size();i++){
					referFileName=new File(listofBaseXmlDirs.get(k)).getParent();
					if(locales_list.get(i).equals("en")){
						referFileName=referFileName+File.separator+"values";
					}else{
						referFileName=referFileName+File.separator+"values-"+locales_list.get(i);
					}
					referFileName=(referdir.equals(""))?referFileName:referFileName.replace(basedir, referdir);
					outFileName=(referdir.equals(""))?referFileName.replace(basedir, outdir):referFileName.replace(referdir, outdir);
					referFileName=referFileName+File.separator+filesToFindMiss.get(j);
					if(baseFileName.equals(referFileName)){
						continue;
					}
					outFileName=outFileName+File.separator+filesToFindMiss.get(j);
					formOutFile(list_srcFile,referFileName,outFileName);
				}
			}
		}
	}
	/**getProjectDir：
	 查找目录STR的顶层工程根目录，例如packages/apps/Settings/res*(/)values*的顶层工程根目录为packages/apps/Settings*/
	String getProjectDir(String STR,ArrayList<String> listofProjectDirs){
		String str_temp=null;
		for(String str:listofProjectDirs){
			if(STR.contains(str)){
				str_temp=str;
				break;
			}
		}
		return str_temp;
	}
	
	public boolean isElementMissed(Element ele_src,List<Element> list_referFile){//比对字符串的tagname、attributevalue等是否相同
		Element ele_refer=null;
		for(int i=0;i<list_referFile.size();i++){
			ele_refer=list_referFile.get(i);
			if(isElementsFormEqual(ele_src,ele_refer)){
				return false;
			}else if(0==isArrayElementEqual){//表明上述数组类型的Element节点ele_src和ele_refer包含的item条目不同
				list_referFile.remove(i);
				i--;
				isReferFileChanged=true;
				isArrayElementEqual=-1;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void formOutFile(ArrayList<Element> list_srcFile,String referFileName,String outFileName){
		List<Element> list_referFile=null;
		boolean isReferFileExist=(new File(referFileName)).exists();
		Element ele_src=null,rootOutFile=null;
		Document doc_referFile=null,doc_outFile=null;
		doc_outFile=DocumentHelper.createDocument();//创建根节点
		rootOutFile=doc_outFile.addElement(FORMAT_RESOURCE);
		rootOutFile.addNamespace(Namespace1,Namespace2);
		rootOutFile.addText(outFileFormat);
		//dayin(ele_src.getXPathResult(0).getText());//数值为：urn:oasis:names:tc:xliff:document:1.2
 		//dayin(ele_src.getNamespaceForPrefix("xliff").asXML());//数值为：xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"
		if(isReferFileExist){
			doc_referFile=getReferFileDoc(referFileName);
			list_referFile=doc_referFile.getRootElement().elements();
		}
		for(int i=0;i<list_srcFile.size();i++){
			ele_src=list_srcFile.get(i);
			if(isReferFileExist&&!isElementMissed(ele_src,list_referFile)){//如果参照文件存在并且包含节点ele_src，表明ele_src不缺失
				continue;
			}
			ele_src.setParent(null);
			rootOutFile.add(ele_src);
			rootOutFile.addText(outFileFormat);
		}
		//如果参照文件中删除了不合适的数组元素节点的话，那么参照文件需更新
		if(isReferFileExist){
			if(isReferFileChanged&&doc_referFile.getRootElement().elements().size()>0){
				writeOutFile(doc_referFile,new File(referFileName));
				isReferFileChanged=false;
			}
		}
		if(doc_outFile.getRootElement().elements().size()>0){//最终的缺失文件必须包含至少一个有效Element类型节点
			writeOutFile(doc_outFile,new File(outFileName));
		}
	}

	public Document getReferFileDoc(String referFileName){
		Document doc=null;
		try{
			doc= saxread.read(new File(referFileName));// 读取XML文件
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return doc;
	}
	
	/**用于判断Element节点标签"name"名称是否出现在文件中：
	    R.string.sim_setting;
		R.array.sim_setting_values;
		R.plurals.plural_sim_settings;
		
		android:title="@string/sim_lock_settings"
		android:entries="@array/lock_after_timeout_entries"
	*/
   public boolean isTagNameInFile(Element ele_src,File file)
    {
	    String TAGNAME=null;
		String tagName=ele_src.getName();
		String attrName=ele_src.attributeValue("name");
		if(null==tagName||attrName==null){
			return false;
		}
		switch(tagName){
			case "string":
				TAGNAME="string";
				break;
			case "string-array":
				TAGNAME="array";
				break;
			case "plurals":
				TAGNAME="plurals";
				break;
			default:
				TAGNAME="";
		}
		try
		{
		    BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
		    BufferedReader reader = new BufferedReader(new InputStreamReader(fis,"utf-8"));
		    String line =null;
		    while((line = reader.readLine()) != null)
		    {
		    	 int rs = line.indexOf(attrName,0);
		         if(rs>=0){
		        	 /**isLengthLT用于消除以下情形：
		        	  * sim_card_settings被引用，判为出现在文件中；但是如果sim_card没有被引用的话，不会因为sim_card_settings/sim_card1/sim_carda被引用而被判为出现在文件中。
		        	  * line.length()==(rs+searchStr.length())||isLengthLT消除以下情形：
		        	  * line的结尾内容为：R.string.sim_setting
		        	 */
		        	 boolean isLengthLT=((rs+attrName.length())<line.length())&&!isCharFormatOK(line.charAt(rs+attrName.length()));
		        	 if(line.length()==(rs+attrName.length())||isLengthLT){
		        		 if(line.contains(TAGNAME)){//用于消除包含R.array.sim_setting而导致string类型的sim_setting(即R.string.sim_setting)被判为引用
			        		 reader.close();
			        		 return true;
		        		 }
		        	 }
		         }
		    }
		    reader.close();
		}catch(Exception e){
		     e.printStackTrace();
		}
        return false;
    }	
	 
   boolean isCharFormatOK(char ch){
	   if(('A'<=ch&&ch<='Z')||('a'<=ch&&ch<='z')||('0'<=ch&&ch<='9')||ch=='_'){
		   return true;
	   }
	   return false;
   }

	public void filterElementsNotUsed(ArrayList<Element> list_srcFile,ArrayList<String> listofLayoutFiles,ArrayList<String> listofSrcFiles){
		if(0==list_srcFile.size()||(0==listofLayoutFiles.size()&&0==listofSrcFiles.size())){
			return;
		}
		Element ele_src=null;
		boolean isFound=false;
		//先查找java类文件
		for(int i=0;i<list_srcFile.size();i++){
			ele_src=list_srcFile.get(i);
			for(String str:listofSrcFiles){
				if(isTagNameInFile(ele_src,new File(str))){
					isFound=true;
					break;
				}
			}
			//再查找布局类xml文件
			if(!isFound){
				for(String str:listofLayoutFiles){
					if(isTagNameInFile(ele_src,new File(str))){
						isFound=true;
						break;
					}
				}
			}
			if(!isFound){
				list_srcFile.remove(i);
				i--;
			}else{
				isFound=false;
			}
		}
		
	}
   
	/**
	* 使用的是FilenameFilter过滤器。 
	*/
	public void getBaseDirs(File baseDir, myFilenameFilter filter) {
		if(!deletedir.equals("")){
			String str_temp=baseDir.getAbsolutePath().replace(basedir, deletedir);
			File file_src=new File(str_temp+File.separator+"src");
			File file_res=new File(str_temp+File.separator+"res");
			if(file_src.exists()&&file_src.isDirectory()&&file_res.exists()&&file_res.isDirectory()){
				addXmlFiles(str_temp,listofProjectDirs);
			}
		}
		File[] files = baseDir.listFiles(); 
		for (File file : files) { 
			if(file.isDirectory()&&isFileNotALink(file)){
				if(isContainExcludePath(file.getName(),exclude_path)){
					continue;
				}
				if(filter.accept(file, file.getAbsolutePath())){
					listofBaseXmlDirs.add(file.getAbsolutePath());
				}else{
					getBaseDirs(file, filter); 
				}
			}
		} 
	} 
	/**
	 * 如下为文件名过滤器的使用
	 * */
	public class myFilenameFilter  implements FilenameFilter{
		public boolean isLocaleDir(String filename){
			String baselocal_name;
			if(baselocale.equals("")||baselocale.equals("en")){
				baselocal_name="values";
			}else{
				baselocal_name="values-"+baselocale;
			}
			return filename.endsWith(baselocal_name);
		 }
		public boolean accept(File dir, String filename) {
			return isLocaleDir(filename);
		}
	}
	
	public void getLayoutSrcFiles(File srcDir, myFilenameFilter_LayoutSrc filter) { 
		File[] files = srcDir.listFiles(); 
		for (File file : files) { 
			if(file.isFile()){
				int index=filter.accept_layoutsrc(file, file.getAbsolutePath());
				if(1==index){
					addXmlFiles(file.getAbsolutePath(),listofLayoutFiles);
				}else if(2==index){
					addXmlFiles(file.getAbsolutePath(),listofSrcFiles);
				}
			}else if(file.isDirectory()){
				getLayoutSrcFiles(file,filter);
			}
		} 
	} 
	
	public class myFilenameFilter_LayoutSrc  implements FilenameFilter{
		public int isLayoutSrcFile(String filename){
			if(filename.contains(File.separator+"xml")||filename.contains(File.separator+"layout")||filename.contains(File.separator+"src")){
				if(filename.endsWith(".xml")){
					return 1;
				}else if(filename.endsWith(".java")){
					return 2;
				}
			}
			return -1;
		 }		
		public boolean accept(File dir, String filename) {
			return false;
		}
		public int accept_layoutsrc(File dir, String filename) {
			return isLayoutSrcFile(filename);
		}
	}
}