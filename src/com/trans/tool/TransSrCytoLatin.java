package com.trans.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TransSrCytoLatin extends TransCommon{
	static String fileName_src="xmlResources\\TranslationSrCytoLatin\\strings.xml";
	static String fileName_target="xmlResources\\TranslationSrCytoLatin\\strings_copy.xml";
    public static void main(String[] args) {
    	if(isInConsoleEnviroment){
    		if (args.length != 2) {
	    		dayin("Please input correct command under Linux environment ! \nsuch as: \"java -cp *.jar com.trans.tool.TransSrCytoLatin from.xml to.xml\"");
	            return;
	        }else{
	        	fileName_src=args[0];
	        	fileName_target=args[1];
	        }
    	}
    	if(new File(fileName_src).exists()&&new File(fileName_src).isFile()){
    		new TransSrCytoLatin();
    	}else{
    		dayin("Please make sure \""+fileName_src+"\" is a file and exist!");
    		return;
    	}
	}    	
    
public TransSrCytoLatin(){
	File file_target=new File(fileName_target);
	
	if(!file_target.getParentFile().exists()){
		file_target.getParentFile().mkdirs();
	}
	transformByReaderWriter(new File(fileName_src),file_target);
}
    
public void transformByReaderWriter(File file_from,File file_to){
    try {  	
        //以字符为单位读取文件内容，一次读一个字节
        Reader reader = new InputStreamReader(new FileInputStream(file_from));
        FileWriter writer = new FileWriter(file_to, false);
        int tempchar;
        int index=0;
        int char_index=0;
        char CH1='\0',CH2='\0';
        SrCytoLatin BBB=new SrCytoLatin();
        
        while ((tempchar = reader.read()) != -1) {
            // 对于windows下，\r\n这两个字符在一起时，表示一个换行。
            // 但如果这两个字符分开显示时，会换两次行。
            // 因此，屏蔽掉\r，或者屏蔽\n。否则，将会多出很多空行。
            //if (((char) tempchar) != '\r'){
            	CH1=CH2;
                CH2=((char) tempchar);
                if(0==index){
                	index=1;
                	continue;
                }
                char_index=BBB.IsMultiChar(CH1);
                if(2==char_index){
                	if(BBB.IsSRCyrillicLowercase(CH2)){
                		writer.write(BBB.SrCytoLatin_ST(CH1,char_index,1));
                	}else{
                		writer.write(BBB.SrCytoLatin_ST(CH1,char_index,-1));    				
                	}
                }
                else if(1==char_index){
                	writer.write(BBB.SrCytoLatin_ST(CH1,char_index,-1));    				
                }
                else{
                    writer.write(BBB.SrCytoLatin_CH(CH1));    			
                } 
        }
        //Now to analyse the last character
        switch(CH2){
        case 'Љ':
        	writer.write("LJ");
        	break;
        case'Њ':
        	writer.write("NJ");
        	break;
        case'Џ':
        	writer.write("DŽ");
        	break;
        case 'љ':
        	writer.write("lj");
        	break;
        case'њ':
        	writer.write("nj");
        	break;
        case'џ':
        	writer.write("dž");
        	break;
        default:
        	writer.write(BBB.SrCytoLatin_CH(CH2)); 
        }//switch
        reader.close();
        writer.close();            
    } catch (Exception e) {
        e.printStackTrace();
    }	
}
/*
SR_Cyrillic_Large=\
(А Б В Г Д Ђ Е Ж З И Ј К Л Љ  М Н Њ  О П Р С Т Ћ У Ф Х Ц Ч Џ  Ш);
SR_Latin_Large=\
(A B V G D Đ E Ž Z I J K L LJ M N NJ O P R S T Ć U F H C Č DŽ Š);

SR_Cyrillic_Large_C=\
(А Б В Г Д Ђ Е Ж З И Ј К Л М Н О П Р С Т Ћ У Ф Х Ц Ч Ш);
SR_Latin_Large_C=\
(A B V G D Đ E Ž Z I J K L M N O P R S T Ć U F H C Č Š);

SR_Cyrillic_Small=\
(а б в г д ђ е ж з и ј к л љ  м н њ  о п р с т ћ у ф х ц ч џ  ш);
SR_Latin_Small=\
(a b v g d đ e ž z i j k l lj m n nj o p r s t ć u f h c č dž š);
*/
public class SrCytoLatin {
	   char [] SRCyrillicUppercase ={'А','Б','В','Г','Д','Ђ','Е','Ж','З','И','Ј','К','Л','Љ','М','Н','Њ','О','П','Р','С','Т','Ћ','У','Ф','Х','Ц','Ч','Џ','Ш'};
	    char [] SRLatinUppercase   ={'A','B','V','G','D','Đ','E','Ž','Z','I','J','K','L','*','M','N','*','O','P','R','S','T','Ć','U','F','H','C','Č','*','Š'};
	    char [] SRCyrillicUppercase_C={'Љ','Њ','Џ'};
	    String [] SRLatinUppercase_CI={"LJ","NJ","DŽ"};
	    String [] SRLatinUppercase_CII={"Lj","Nj","Dž"};
	    /////////////////////////////////////////////////////
	    char [] SRCyrillicLowercase={'а','б','в','г','д','ђ','е','ж','з','и','ј','к','л','љ','м','н','њ','о','п','р','с','т','ћ','у','ф','х','ц','ч','џ','ш'};
	    char [] SRLatinLowercase   ={'a','b','v','g','d','đ','e','ž','z','i','j','k','l','*','m','n','*','o','p','r','s','t','ć','u','f','h','c','č','*','š'};	
	    char [] SRCyrillicLowercase_C={'љ','њ','џ'};
	    String [] SRLatinLowercase_C={"lj","nj","dž"};

	public char SrCytoLatin_CH(char ch)
	{
		int count;
		for(count=0;count<SRCyrillicLowercase.length;count++)
		{
			if(ch==SRCyrillicLowercase[count]){
				return SRLatinLowercase[count];
			}
		}
		for(count=0;count<SRCyrillicUppercase .length;count++)
		{
			if(ch==SRCyrillicUppercase [count]){
				return SRLatinUppercase[count];
			}
		}    	
		return ch;
	}
	boolean IsSRCyrillicLowercase(char ch){
		int count;
		for(count=0;count<SRCyrillicLowercase.length;count++){
			if(ch==SRCyrillicLowercase[count]){
				return true;
			}
		}
		return false;
	}
	int IsMultiChar(char ch){
		int count;
		for(count=0;count<SRCyrillicLowercase_C.length;count++){
			if(ch==SRCyrillicLowercase_C[count]){
				return 1;
			}
		}
		
		for(count=0;count<SRCyrillicUppercase_C.length;count++){
			if(ch==SRCyrillicUppercase_C[count]){
				return 2;
			}
		}
		//This mean that ch is not a multi-one
		return 0;
	}
	public String SrCytoLatin_ST(char ch,int index,int index_large)
	{
		StringBuffer SR_temp=new StringBuffer();
		int count;
		if(1==index){
	    	for(count=0;count<SRCyrillicLowercase_C.length;count++){
	    		if(ch==SRCyrillicLowercase_C[count]){
	    			SR_temp.append(SRLatinLowercase_C[count]);
	    		}
	    	}
		}
		else if(2==index){
	    	for(count=0;count<SRCyrillicUppercase_C.length;count++){
	    		if(ch==SRCyrillicUppercase_C[count]){
	    			SR_temp.append((1==index_large)?SRLatinUppercase_CII[count]:SRLatinUppercase_CI[count]);
	    		}
	    	}	
		}
		return SR_temp.toString();
	}
}

public static void dayin(Object STR)
{
	System.out.println(STR);
}	
}
