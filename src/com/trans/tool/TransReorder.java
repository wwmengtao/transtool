package com.trans.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Reorder the string: 将product="default"字串调整到product="tablet"下面，以便系统编译的时候使用product="default"属性。
 * @author Mengtao1
 *
 */
public class TransReorder extends TransImport{
	private String attrName = null;
	private String attrValueAbove = null;
	private String attrValueBelow = null;
	private ArrayList<Element> listOrdered = null;//Elements to be added at the file end
	private boolean remainElements = false;
	public static void main(String []args){
		File configFile = getConfigFile(args,fileName_reorder,isInConsoleEnviroment);	
		if(null!=configFile){
			new TransReorder(configFile);
		}
	}
	
	public TransReorder(File configFile){
		ParseConfigFile(configFile);
	}
	
	public String getSingleTagValue(Document doc, String tagPath){
		if(null==doc || null==tagPath)return null;
		String tagValue = null;
		List<Element> list_configFile;
		list_configFile=doc.selectNodes(tagPath);
		tagValue=list_configFile.get(0).getData().toString().trim();
		return tagValue;
	}
	
	public boolean isNullString(String str){
			return (null==str || str.equals(""));
	}
	
	@Override
	public void ParseConfigFile(File configFile){
		listofSrcXmlFiles=new ArrayList<File>();
		try{
			Document doc= saxread.read(configFile);//读取XML文件
			srcdir=getSingleTagValue(doc,"/config/srcdir");
			String strDelEle=getSingleTagValue(doc,"/config/remainElements");
			if(!strDelEle.toLowerCase().equals("false")){
				remainElements = true;
			}
			attrName=getSingleTagValue(doc,"/config/attrName");
			attrValueAbove=getSingleTagValue(doc,"/config/attrValueAbove");
			attrValueBelow=getSingleTagValue(doc,"/config/attrValueBelow");
			if(isNullString(srcdir)||isNullString(strDelEle)||isNullString(attrName)||isNullString(attrValueAbove)||isNullString(attrValueBelow)){
				dayin("Parameters in reorder_translation.xml null!");
				return;
			}
			dayin("attrValueAbove:"+attrValueAbove);
			dayin("attrValueBelow:"+attrValueBelow);
			reorderTranslations();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void reorderTranslations(){
		myFilenameFilter myfilter = new myFilenameFilter(); 
		String reorder_file;
		getSrcXmlFiles(new File(srcdir),myfilter);
		//dayin(srcdir);
		if(null==listofSrcXmlFiles||0==listofSrcXmlFiles.size())return;
		for(int i=0;i<listofSrcXmlFiles.size();i++)	{
			reorder_file=listofSrcXmlFiles.get(i).getAbsolutePath();
			reorderFile(reorder_file);
		}
	}

	public void reorderFile(String reorder_file){
		ArrayList<Element> list_srcFile=null;
		try{
			Document doc_srcFile = saxread.read(new File(reorder_file));
			reorderElements(doc_srcFile);
			if(doc_srcFile.getRootElement().elements().size()>0){
				writeOutFile(doc_srcFile,new File(reorder_file));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void printListItems(List list_src){
		if(null!=list_src&&list_src.size()>0){
			for(int i=0;i<list_src.size();i++){
				dayin(((Element)list_src.get(i)).getTextTrim());
			}
		}
	}
	
	public void reorderElements(Document doc_srcFile){
		if(remainElements)listOrdered = new ArrayList<Element>();
		Element eleRoot=(Element)doc_srcFile.getRootElement();
		List list_src=eleRoot.elements();
		Attribute mAttrSrc, mAttrOrder= null;
		String mAttrValueSrc, mAttrValueOrder = null;
		Element ele_src=null, ele_temp=null;
		for(int i=0;list_src.size()>1 && i<list_src.size();i++){
			ele_src=(Element)list_src.get(i);
			mAttrSrc = ele_src.attribute(attrName);
			if(null!=mAttrSrc){
				mAttrValueSrc = mAttrSrc.getData().toString().toLowerCase();
				if(null!=mAttrValueSrc && mAttrValueSrc.equals(attrValueAbove)){
					list_src.remove(ele_src);
					ele_src.setParent(null);
					//printListItems(list_src);
					if(remainElements)listOrdered.add(ele_src);
					i--;
				}//if
			}//if
		}//for
		//printListItems(listOrdered);
		if(remainElements){//如果attrName和attrValue对应的字串保留的话，那么就追加到文件末尾
			Element ele_order=null;
			String tag_order=null, tag_src=null;
			String attrName2 = "name";
			for(int i=0;listOrdered.size()>0 && i<listOrdered.size();i++){
				ele_order = (Element)listOrdered.get(i);
				if(null==(mAttrOrder = ele_order.attribute(attrName2))){
					continue;
				}
				tag_order = mAttrOrder.getData().toString();
				for(int j=0;list_src.size()>0 && j<list_src.size();j++){
					ele_src=(Element)list_src.get(j);		
					if(null==(mAttrSrc = ele_src.attribute(attrName2))){
						continue;
					}
					tag_src = mAttrSrc.getData().toString();
					if(null==(mAttrSrc = ele_src.attribute(attrName))){
						continue;
					}
					if(tag_order.equals(tag_src)){
						mAttrValueSrc = mAttrSrc.getData().toString().toLowerCase();
						if(null!=mAttrValueSrc && mAttrValueSrc.equals(attrValueBelow)){
							dayin("equal tags:"+tag_order);
							ele_order.setParent(null);
							list_src.add(j,ele_order);
							listOrdered.remove(ele_order); i--;
							j++;
						}//if
					}//if
				}//for
			}
			//eleRoot.setContent(list_src);
			for(int i=0;i<listOrdered.size();i++){
				eleRoot.add((Element)listOrdered.get(i));
				eleRoot.addText(outFileFormat);	
			}//for
		}//if


	}
	
	public void writeOutFile(Document doc_outFile,File outFile){
		try{
				if(!outFile.getParentFile().exists()){
					outFile.getParentFile().mkdirs();
				}
				OutputFormat of=new OutputFormat("",false);//用于控制节点的输出格式
				of.setEncoding("utf-8");
				of.setNewlines(true); //设置是否换行
				of.setTrimText(true);//删除多余的空格
				//XMLWriter writer = new XMLWriter(new FileWriter(outFile),of);//写入文件
                StringWriter sw = new StringWriter(); 
                XMLWriter writer = new XMLWriter(sw, of); //写入输出流
				writer.write(doc_outFile);
				writer.close();
				dayin("###############################################################");
				dayin(sw.toString());//将输出流的内容打印出来
	    }catch(Exception ex){
	    	ex.printStackTrace();
	    }
	}		
}
