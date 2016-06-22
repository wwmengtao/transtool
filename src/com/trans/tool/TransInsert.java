package com.trans.tool;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class TransInsert  extends TransCommon{
	//以下定义变量存储数值
    ArrayList<String> srcStringIDList=null;
    ArrayList<String> targetStringIDList=null;
    ArrayList<String> srcStringarrayIDList=null;
    ArrayList<String> targetStringarrayIDList=null;
    ArrayList<String> srcpluralsIDList=null;
    ArrayList<String> targetpluralsIDList=null;    
    //
	public static void main(String []args){
		File configFile = getConfigFile(args,fileName_insert,isInConsoleEnviroment);	
		if(null!=configFile){
			new TransInsert(configFile);
		}
	}
	
	public TransInsert(File configFile){
		parseConfigFile(configFile);
	}
	
	public void parseConfigFile(File configFile){
		try{
			//以下定义配置文件所需的变量
			String srcPath=null;
			String srcFile=null;
			String targetPath=null;
			String targetFile=null;			
		    ArrayList<String> localesList=null;
			Document doc= saxread.read(configFile);// 读取XML文件
			Element root=doc.getRootElement();
			List list = root.elements();
			for(Iterator its =  list.iterator();its.hasNext();){   
	            Element ele = (Element)its.next();
	            String strValue=ele.getData().toString().trim();
	            switch(ele.getName()){
		            case "srcPath":
		            	srcPath=strValue;	
		            	if(!isDirProper(srcPath,"srcPath")){return;}
		            	break;
		            case "srcFile":
		            	srcFile=strValue;	
		            	break;
		            case "srcStringID":
		            	srcStringIDList = getStringTokenizer(strValue);	
		            	break;
		            case "srcStringarrayID":
		            	srcStringarrayIDList = getStringTokenizer(strValue);	
		            	break;
		            case "srcpluralsID":
		            	srcpluralsIDList = getStringTokenizer(strValue);	
		            	break;		            	
		            case "targetPath":
		            	targetPath=strValue;	
		            	break;
		            case "targetFile":
		            	targetFile=strValue;	
		            	break;
		            case "targetStringID":
		            	targetStringIDList = getStringTokenizer(strValue);	
		            	break;
		            case "targetStringarrayID":
		            	targetStringarrayIDList = getStringTokenizer(strValue);	
		            	break;	 
		            case "targetpluralsID":
		            	targetpluralsIDList = getStringTokenizer(strValue);	
		            	break;				            	
		            case "locales":
		            	localesList = getStringTokenizer(strValue);
		            	break;	   
		            default:
		            	break;
	            }
	        }      
		    if(srcStringIDList.size()!=targetStringIDList.size()){
		    	dayin("The count of srcStringID not equals with targetStringID!");
		    	return;
		    }
		    if(srcStringarrayIDList.size()!=targetStringarrayIDList.size()){
				dayin("The count of srcStringarrayID not equals with targetStringarrayID!");
				return;
		    }
		    if(srcpluralsIDList.size()!=targetpluralsIDList.size()){
				dayin("The count of srcpluralsIDList not equals with targetpluralsIDList!");
				return;
		    }
		    if(0==localesList.size()){
		    	dayin("locales can not be null!");
		    	return;
		    }
		    if(!(new File(srcPath)).exists()){
		    	dayin("Please confirm srcPath exist!");
		    	return;
		    }
			File path_srcFile;
			File path_targetFile;
			for(int i=0;i<localesList.size();i++){
				if("en".equals(localesList.get(i).toString())){
					path_srcFile=new File(srcPath+File.separator+"res"+File.separator+"values"+File.separator+srcFile);
					path_targetFile=new File(targetPath+File.separator+"res"+File.separator+"values"+File.separator+targetFile);
				}
				else{
					path_srcFile=new File(srcPath+File.separator+"res"+File.separator+"values-"+localesList.get(i).toString()+File.separator+srcFile);
					path_targetFile=new File(targetPath+File.separator+"res"+File.separator+"values-"+localesList.get(i).toString()+File.separator+targetFile);
				}
				if(!path_srcFile.exists()){
					continue;
				}
			    InsertTranslations(path_srcFile,path_targetFile);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}	
	}

	/**
	InsertTranslations：此种方法核心是将string元素和string-array元素分开查找，并且分别替换或追加
	*/
	@SuppressWarnings({ "unused", "unchecked" })
	public void InsertTranslations(File path_srcFile,File path_targetFile){
		try{
				Document doc_srcFile = saxread.read(path_srcFile);
				String XMLEncoding=doc_srcFile.getDocument().getXMLEncoding();
				List<Element> srcFileList_string = doc_srcFile.selectNodes("/resources/string"); 
				List<Element> srcFileList_stringarray = doc_srcFile.selectNodes("/resources/string-array"); 
				List<Element> srcFileList_pluralsarray = doc_srcFile.selectNodes("/resources/plurals");
				Document doc_targetFile=null;
				Element eleRoot=null;
				if(0==srcFileList_string.size()&&0==srcFileList_stringarray.size()&&0==srcFileList_pluralsarray.size()){
					return;
				}else if(!path_targetFile.exists()){
					doc_targetFile=DocumentHelper.createDocument();//创建根节点
					eleRoot=doc_targetFile.addElement(FORMAT_RESOURCE);
					eleRoot.addNamespace(Namespace1,Namespace2);
					eleRoot.addText(outFileFormat);
				}else{
					doc_targetFile = saxread.read(path_targetFile);	
				}
				List<Element> srcFileList = new ArrayList<Element>();
				addCertainElements(srcFileList,srcFileList_string,srcStringIDList,targetStringIDList);
				addCertainElements(srcFileList,srcFileList_stringarray,srcStringarrayIDList,targetStringarrayIDList);
				addCertainElements(srcFileList,srcFileList_pluralsarray,srcpluralsIDList,targetpluralsIDList);
				if(0==srcFileList.size()){
					return;
				}
				insertElements(srcFileList,doc_targetFile);
				writeOutFile(doc_targetFile,path_targetFile);		
		}catch(DocumentException e){
			e.printStackTrace();
		}
	}
	
	public List<Element> addCertainElements(List<Element> srcFileList,List<Element> srcFileList_strings,ArrayList<String> srcIDList,ArrayList<String> targetIDList){
		if(0!=srcFileList_strings.size()&&0!=srcIDList.size()){
			String stringID_SRC;
			Element ele_node=null;
			for(int i=0;i<srcIDList.size();i++){
				stringID_SRC=srcIDList.get(i);
				for(Element ele_temp:srcFileList_strings){
					ele_node=(Element)ele_temp.clone();
					if(ele_node.attribute("name").getData().equals(stringID_SRC)){
						ele_node.attribute("name").setText(targetIDList.get(i));
						srcFileList.add(ele_node);
					}
				}
			}
		}
		return srcFileList;
	}
	/**
	InsertElements：分别对两类节点(string和string-array节点)做替换或者追加
	一、以下方法也可以替换节点，但是不能保证替换后的节点顺序
	Element eleparentTemp=eleTemp.getParent();
	eleparentTemp.remove(eleTemp);
	elemapTemp.setParent(null);	
	eleparentTemp.add(elemapTemp);
	二、下列方法也可以增加节点，但是过于繁琐
	Attribute attr_map;
	Element ele_string;
	ele_string = eleRootTemp.addElement("string");
	ele_string.setText((String)ele_map.getData());
	attr_map=ele_map.attribute("product");
	dayin(attr_map.getData());
	ele_string.addAttribute("name", str_iter);
	*/
	public void InsertElements(HashMap<String, Element> map_targetFilestring,List<Element> targetFileList_string,Element eleRoot){
		String namevalue,attributevalue;
		Element eleTemp,elemapTemp;
		String xpath_ele;
		List<Element> list_ele ;
		List list_index=eleRoot.elements();
		for(int j=0;j<targetFileList_string.size();j++){
			if(0==map_targetFilestring.size()){
				break;
			}
			namevalue=targetFileList_string.get(j).getName();
			attributevalue=(String)targetFileList_string.get(j).attribute("name").getData();
			if(map_targetFilestring.containsKey(attributevalue)){
				//下列list_ele为所有<namevalue name="attributevalue"...>...</namevalue>形式的节点集合
				xpath_ele="//"+namevalue+"[@name='"+attributevalue+"']";
				list_ele=eleRoot.selectNodes(xpath_ele);
    			elemapTemp=map_targetFilestring.get(attributevalue);
    			elemapTemp.setParent(null);
				//此处的循环表示list_ele节点集合内所有节点都进行替换
				for (int k = 0; k < list_ele.size(); k++){	
					eleTemp=list_ele.get(k);
					if(isElementsFormEqual(eleTemp,elemapTemp)){
						list_index.set(list_index.indexOf(eleTemp),elemapTemp);
						map_targetFilestring.remove(attributevalue);
					}
	            }
			}
		}
		//以下将map_targetFilestring中剩余Element节点追加到eleRoot末尾
		if(0!=map_targetFilestring.size()){		
			Element ele_map;
			String str_iter;
			for(Iterator iter=map_targetFilestring.keySet().iterator();iter.hasNext();){
				str_iter=(String)iter.next();
				ele_map=map_targetFilestring.get(str_iter);
				ele_map.setParent(null);
				eleRoot.add(ele_map);
				eleRoot.addText(outFileFormat);				
			}
		}
	}

public HashMap<String, Element> getMap(ArrayList<String> srcStringIDList,ArrayList<String> targetStringIDList,List<Element> srcFileList_string){
	int index1=0;
	HashMap<String, Element> map_srcFilestring= new HashMap<String, Element>();
	Element ele_srcFile,ele_temp;
	if(0!=srcStringIDList.size()){
		for (int j = 0; j < srcFileList_string.size(); j++) {
			if(index1==srcStringIDList.size()){break;}
			ele_srcFile=srcFileList_string.get(j);
			for(int k=0;k<srcStringIDList.size();k++){
				if(srcStringIDList.get(k).equals(ele_srcFile.attribute("name").getData())){
					ele_temp=ele_srcFile;
					ele_temp.attribute("name").setText(targetStringIDList.get(k));
					map_srcFilestring.put(targetStringIDList.get(k),ele_temp);
					index1++;
					break;
				}
			}
		}
	}
	return map_srcFilestring;
}	
}
