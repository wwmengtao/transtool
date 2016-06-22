package com.trans.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TransFileMerge extends TransCommon{
	//arrl_baseXmlFiles：存储的是基础文件(即其它文件可能并入其中)，例如strings.xml、arrays.xml
	ArrayList<String> arrl_baseXmlFiles;
	ArrayList<ArrayList<String>>arrl_subXmlFiles;
	String mergeDir=null;
	public static void main(String []args){
		if(!new File(fileName_filter).exists()){
			dayin("Please make sure \""+fileName_filter+"\" exists!");
			return;
		}
		new TransFileMerge();
	}

	@SuppressWarnings("unchecked")
	public TransFileMerge(){
		arrl_baseXmlFiles=new ArrayList<String>();
		try{
			Document doc= saxread.read(new File(fileName_filter));// 读取XML文件
			List<Element> list_baseXmlFiles=doc.getRootElement().elements();
			List<Element> list_subXmlFiles;
			List<Element> list_mergeDir=doc.selectNodes("/filename/mergeDir");	
			mergeDir=list_mergeDir.get(0).getData().toString().trim();
			if(!isDirProper(mergeDir,"mergeDir")){return;}
			String str_temp;
			int size_list_base=list_baseXmlFiles.size();
			arrl_subXmlFiles=new ArrayList<ArrayList<String>>(size_list_base);
			for(int i=0;i<size_list_base;i++){
				str_temp=list_baseXmlFiles.get(i).getName();
				arrl_baseXmlFiles.add(str_temp);
				list_subXmlFiles=doc.selectNodes("/filename/"+str_temp+"/file");
				ArrayList<String> xmlFiles_temp=new ArrayList<String>();
				addXmlFiles(list_subXmlFiles,xmlFiles_temp);
				arrl_subXmlFiles.add(xmlFiles_temp);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		getBaseDirs(new File(mergeDir),new myFilenameFilter());
		
	}
	public void startMergeFiles(File file){
		Document doc_Base=null;
		Element eleRoot_Base=null;
		Document doc_Sub=null;
		File File_Base=null,File_Sub=null;
		String file_base=null,fille_sub=null;
		int size_element=0;
		try{
			for(int i=0;i<arrl_baseXmlFiles.size();i++){
				file_base=file+File.separator+arrl_baseXmlFiles.get(i);
				File_Base=new File(file_base);
				for(int j=0;j<arrl_subXmlFiles.get(i).size();j++){
					fille_sub=file+File.separator+arrl_subXmlFiles.get(i).get(j);
					File_Sub=new File(fille_sub);
					if(!File_Sub.exists()){
						continue;
					}else if(!File_Base.exists()){
						File_Sub.renameTo(File_Base);
						continue;
					}
					if(null==doc_Base){
						doc_Base= saxread.read(File_Base);// 读取XML文件
						eleRoot_Base=(Element)doc_Base.getRootElement();
						size_element=eleRoot_Base.elements().size();
					}
					doc_Sub= saxread.read(File_Sub);
					MergeFile(eleRoot_Base,doc_Sub);
					File_Sub.delete();
				}
				if(null!=eleRoot_Base&&eleRoot_Base.elements().size()>size_element){//文件如果至少增加了一个有效Element类型节点
					writeOutFile(doc_Base,File_Base);
				}
				doc_Base=null;
				eleRoot_Base=null;
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public void MergeFile(Element eleRoot_Base,Document doc_Sub){//文件合并时，元素节点只追加不替换
		Element eleRoot_Sub=(Element)doc_Sub.getRootElement();
		List list_Base=eleRoot_Base.elements();
		List list_Sub=eleRoot_Sub.elements();
		Element ele_Base=null,ele_Sub=null;
		boolean isNeedAdded;
		for(int i=0;i<list_Sub.size();i++){
			isNeedAdded=true;//先默认源文件中每个节点都需要追加到目标文件
			ele_Sub=(Element)list_Sub.get(i);
			for(int j=0;j<list_Base.size();j++){
				ele_Base=(Element)list_Base.get(j);
				if(isElementsAttrEqual(ele_Base,ele_Sub)){
					isNeedAdded=false;
					break;
				}
			}
			if(isNeedAdded){
				ele_Sub.setParent(null);
				eleRoot_Base.add(ele_Sub);
				eleRoot_Base.addText(outFileFormat);
			}
		}
	}
	
	/**
	* 使用的是FilenameFilter过滤器。 
	*/ 
	public void getBaseDirs(File srcDir, myFilenameFilter filter) { 
		File[] files = srcDir.listFiles(); 
		for (File file : files) {
			if(file.isDirectory()){
				if(filter.accept(file, file.getAbsolutePath())){
					startMergeFiles(file);
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
			if(filename.contains(File.separator+"values")){
				return true;
			}
			return false;
		 }
		public boolean accept(File dir, String filename) {
			return isLocaleDir(filename);
		}
	}
	
}