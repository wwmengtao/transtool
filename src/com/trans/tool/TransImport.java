package com.trans.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TransImport extends TransCommon{
	String srcdir,targetdir;
	ArrayList<File> listofSrcXmlFiles;
	public static void main(String []args){
		File configFile = getConfigFile(args,fileName_import,isInConsoleEnviroment);	
		if(null!=configFile){
			new TransImport(configFile);
		}
	}
	
	public TransImport(File configFile){
		ParseConfigFile(configFile);
	}
	
	@SuppressWarnings("unchecked")
	public void ParseConfigFile(File configFile){
		try{
			Document doc= saxread.read(configFile);//读取XML文件
			List<Element> list_configFile;
			list_configFile=doc.selectNodes("/config/srcdir");
			srcdir=list_configFile.get(0).getData().toString().trim();
			if(!isDirProper(srcdir,"srcdir")){return;}
			list_configFile=doc.selectNodes("/config/targetdir");	
			targetdir=list_configFile.get(0).getData().toString().trim();
			listofSrcXmlFiles=new ArrayList<File>();
			importTranslations();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void importTranslations(){
		myFilenameFilter myfilter = new myFilenameFilter(); 
		String import_srcFile,import_targetFile;
		getSrcXmlFiles(new File(srcdir),myfilter);
		//long startTime = System.currentTimeMillis();  
		for(int i=0;i<listofSrcXmlFiles.size();i++)	{
			import_srcFile=listofSrcXmlFiles.get(i).getAbsolutePath();
			import_targetFile=import_srcFile.replace(srcdir, targetdir);
			if(new File(import_targetFile).exists()){
				importFile(import_srcFile,import_targetFile);
			}else{
				selectToCreateFile(import_srcFile,import_targetFile);
			}
		}
		//long endTime = System.currentTimeMillis();		dayin("程序运行时间："+(endTime - startTime));		
	}
	
	public void importFile(String import_srcFile,String import_targetFile){
		ArrayList<Element> list_srcFile=null;
		try{
			Document doc_srcFile = saxread.read(new File(import_srcFile));
			Document doc_targetFile = saxread.read(new File(import_targetFile));
			List list_src=((Element)doc_srcFile.getRootElement()).elements();
			insertElements(list_src,doc_targetFile);
			if(doc_targetFile.getRootElement().elements().size()>0){
				writeOutFile(doc_targetFile,new File(import_targetFile));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	* 使用的是FilenameFilter过滤器。 
	*/ 
	public void getSrcXmlFiles(File srcDir, myFilenameFilter filter) { 
		File[] files = srcDir.listFiles(); 
		for (File file : files) { 
			if(!file.isDirectory()){
				if(filter.accept(file, file.getAbsolutePath())){
					listofSrcXmlFiles.add(file);
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
			ArrayList<String> filesToImport=getInitialXmlFiles();
			for(int i=0;i<filesToImport.size();i++){
				if(filename.endsWith(filesToImport.get(i))){
					return true;
				}
			}
			return false;
		 }
		public boolean accept(File dir, String filename) {
			return isXmlFile(filename);
		}
	}
}
