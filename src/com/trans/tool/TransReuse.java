package com.trans.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TransReuse extends TransCommon{
	String srcdir,targetdir,outdir;
	ArrayList<String> locales_list;
	List exclude_path;
	ArrayList<File> listofSrcXmlDirs;
	public static void main(String []args){	
		File configFile = getConfigFile(args,fileName_reuse,isInConsoleEnviroment);	
		if(null!=configFile){
			new TransReuse(configFile);
		}
	}

	public TransReuse(File configFile){
		ParseConfigFile(configFile);
	}	

	@SuppressWarnings("unchecked")
	public void ParseConfigFile(File configFile){
		try{
			Document doc= saxread.read(configFile);// 读取XML文件
			List<Element> list_configFile;
			String locales;
			list_configFile=doc.selectNodes("/config/srcdir");
			srcdir=list_configFile.get(0).getData().toString().trim();
			if(!isDirProper(srcdir,"srcdir")){return;}
			list_configFile=doc.selectNodes("/config/targetdir");
			targetdir=list_configFile.get(0).getData().toString().trim();
			if(!isDirProper(targetdir,"targetdir")){return;}
			list_configFile=doc.selectNodes("/config/outdir");
			outdir=list_configFile.get(0).getData().toString().trim();
			if(isNeedRefreshOutDir){
				refreshOutDir(outdir);
			}
			list_configFile=doc.selectNodes("/config/locales");
			locales=list_configFile.get(0).getData().toString().trim();			
			locales_list = getStringTokenizer(locales);
			exclude_path=doc.selectNodes("/config/exclude-path");
		    if(0!=locales_list.size()){
				listofSrcXmlDirs=new ArrayList<File>();
			    findReusedTranslations();
		    }
			
		}catch(Exception ex){
			ex.printStackTrace();
		}					
	}
	
	public void findReusedTranslations(){
		ArrayList<String> filesToReuse=getInitialXmlFiles();
		myFilenameFilter myfilter = new myFilenameFilter(); 
		getSrcDirs(new File(srcdir),myfilter);
		String reuse_srcFile,reuse_targetFile;
		ArrayList<Element> list_srcFile=null,list_targetFile=null;
		for(int i=0;i<listofSrcXmlDirs.size();i++)	{
			for(int j=0;j<filesToReuse.size();j++)	{
				reuse_srcFile=listofSrcXmlDirs.get(i)+File.separator+filesToReuse.get(j);
				reuse_targetFile=reuse_srcFile.replace(srcdir, targetdir);
				if(new File(reuse_srcFile).exists()&&new File(reuse_targetFile).exists()){//如果待比较的两个文件都存在
					list_srcFile=gatherNodesProper(new File(reuse_srcFile),false);
					list_targetFile=gatherNodesProper(new File(reuse_targetFile),false);
					startToReuse(list_srcFile,list_targetFile,reuse_srcFile,reuse_targetFile);
				}
			}
		}
	}
	
	public void startToReuse(ArrayList<Element> list_srcFile,ArrayList<Element> list_targetFile,String reuse_srcFile,String reuse_targetFile){
		Document doc_srcFile,doc_targetFile;
		Element root_srcFile,root_targetFile;
		Element ele_src=null,ele_target=null;
		String out_srcFile,out_targetFile;
		doc_srcFile=DocumentHelper.createDocument();//创建根节点
		root_srcFile=doc_srcFile.addElement(FORMAT_RESOURCE);
		root_srcFile.addNamespace(Namespace1,Namespace2);
		///////////////////////////////////////////////////
		doc_targetFile=DocumentHelper.createDocument();//创建根节点
		root_targetFile=doc_targetFile.addElement(FORMAT_RESOURCE);
		root_targetFile.addNamespace(Namespace1,Namespace2);
		while(0!=list_srcFile.size()&&0!=list_targetFile.size()){
			ele_src=list_srcFile.get(0);
			list_srcFile.remove(ele_src);			
			for(int i=0;i<list_targetFile.size();i++){
				ele_target=list_targetFile.get(i);
				if(isEleNodesCanBeReused(ele_src,ele_target)){
					root_srcFile.addText(outFileFormat);
					root_srcFile.add(ele_src);
					root_targetFile.addText(outFileFormat);
					root_targetFile.add(ele_target);
					list_targetFile.remove(ele_target);					
					break;
				}
			}//for
		}//while
		if(root_srcFile.elements().size()>0){
			root_srcFile.addText(outFileFormat);
			root_targetFile.addText(outFileFormat);
			out_srcFile=reuse_srcFile.replace(srcdir, outdir+File.separator+"src");
			out_targetFile=reuse_targetFile.replace(targetdir, outdir+File.separator+"target");
			writeOutFile(doc_srcFile,new File(out_srcFile));
			writeOutFile(doc_targetFile,new File(out_targetFile));
		}
	}
	
	public boolean isEleNodesCanBeReused(Element ele_src,Element ele_target){//找到标签名属性值相同但是内容不同的元素节点
		String tagname_src=ele_src.getName();
		if(isElementsFormEqual(ele_src, ele_target)){//待比较节点需满足形式上的相似，否则无需比较
			if(tagname_src.equals("string")){
				if(!isStringFormEqual(ele_src.getStringValue().trim(),ele_target.getStringValue().trim())){
					return true;
				}
			}else if(tagname_src.equals("string-array")||tagname_src.equals("plurals")){
				if(isArrayElementsCanBeReused(ele_src,ele_target)){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	* 使用的是FilenameFilter过滤器。 
	*/ 
	public void getSrcDirs(File srcDir, myFilenameFilter filter) { 
		File[] files = srcDir.listFiles(); 
		for (File file : files) { 
			if(file.isDirectory()&&!isContainExcludePath(file.getName(),exclude_path)){
				if(filter.accept(file, file.getAbsolutePath())){
					listofSrcXmlDirs.add(file);
				}
				else{
					getSrcDirs(file, filter); 
				}//if
			}//if
		}//for
	}
	/**
	 * 如下为文件名过滤器的使用
	 * */
	public class myFilenameFilter  implements FilenameFilter{
		public boolean isLocaleDir(String filename){
			String local,local_temp;
			for(int i=0;i<locales_list.size();i++){
				local=locales_list.get(i);
				if(local.equals("en")){
					local_temp="values";
				}
				else{
					local_temp="values-"+local;
				}
				if(filename.endsWith(local_temp)){
					return true;
				}
			}
			return false;
		 }
		public boolean accept(File dir, String filename) {
			return isLocaleDir(filename);
		}
	}	

}
