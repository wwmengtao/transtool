package com.trans.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.trans.tool.TransFileMerge.myFilenameFilter;

public class TransFileFilter  extends TransCommon{
	//arrl_baseXmlFiles：存储的是基础文件(即其它文件可能并入其中)，例如strings.xml、arrays.xml
	ArrayList<String> XmlFiles;
	String mergeDir=null;
	public static void main(String []args){
		if(!new File(fileName_filter).exists()){
			dayin("Please make sure \""+fileName_filter+"\" exists!");
			return;
		}
		new TransFileFilter();
}
	public TransFileFilter(){
		XmlFiles=new ArrayList<String>();
		try{
			Document doc= saxread.read(new File(fileName_filter));// 读取XML文件
			List<Element> list_mergeDir=doc.selectNodes("/filename/mergeDir");	
			mergeDir=list_mergeDir.get(0).getData().toString().trim();
			if(!isDirProper(mergeDir,"mergeDir")){return;}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		getXmlFiles(new File(mergeDir),new myFilenameFilter());
		dayin("The xml files under \""+mergeDir+"\" are shown as following:");
		for(int i=0;i<XmlFiles.size();i++){
			dayin(XmlFiles.get(i));
		}
	}
	
	/**
	* 使用的是FilenameFilter过滤器。 
	*/ 
	public void getXmlFiles(File srcDir, myFilenameFilter filter) { 
		File[] files = srcDir.listFiles(); 
		try{
			for (File file : files) {
				if(file.isFile()){
					if(filter.accept(file, file.getAbsolutePath())){
						addXmlFiles(file.getName(),XmlFiles);
					}
				}else{
					getXmlFiles(file, filter);
				}
			} 
		}catch(NullPointerException e){
			//此处的异常，比如某个文件夹下的某个链接对应的文件夹不存在，这样for (File file : files)就会产生空指针异常
		}
	}
	/**
	 * 如下为文件名过滤器的使用
	 * */
	public class myFilenameFilter  implements FilenameFilter{
		public boolean isLocaleDir(String filename){
			if(filename.endsWith(".xml")){
				return true;
			}
			return false;
		 }
		public boolean accept(File dir, String filename) {
			return isLocaleDir(filename);
		}
	}	
}