package com.trans.tool;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
R.string.sim_setting;
R.plurals.plural_sim_settings;
R.array.sim_setting_values;

android:title="@string/sim_lock_settings"
android:entries="@array/lock_after_timeout_entries"
*/
public class TransSearch
{
        private int lineNum = 0;
        private String path = "";
        private String searchStr = "";
        public void setPath(String value)
        {
                path = value;
        }
        public String getPath()
        {
                return path;
        }
        public void setSearchStr(String value)
        {
                searchStr = value;
        }
        public String getSearchStr()
        {
                return searchStr;
        }
        /**
         * Java search by index
         */
        public void start()
        {
                if(null == path || path.length()<1)
                        return;
                try
                {
                        long startMili=System.currentTimeMillis();
                        System.out.println("Start search \""+searchStr+"\" in file: "+path);
                        File file = new File(path);
                        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fis,"utf-8"));
                        String line = "";
                        lineNum = 0;
                        while((line = reader.readLine()) != null)
                        {
                                lineNum ++;
                                String rs = this.searchStr(line, searchStr);
                                if(rs.length()>0)
                                {
                                //      System.out.println("Find in Line["+lineNum+"], index: "+rs);
                                }
                        }
                        reader.close();
                        System.out.println("Finished!");
                        long endMili=System.currentTimeMillis();
                        System.out.println("Total times: "+(endMili-startMili)+" ms");
                        System.out.println("");
                }
                catch(Exception e)
                {
                        e.printStackTrace();
                }
        }
        /**
         * Call shell command to search
         */
        public void startByShell()
        {
                try
                {
                        long startMili=System.currentTimeMillis();
                        System.out.println("Start search \""+searchStr+"\" in file: "+path+ " by shell");
                        String[] cmd = {"/bin/sh", "-c", "grep "+searchStr+" "+path+" -n "};
                        Runtime run = Runtime.getRuntime();
                        Process p = run.exec(cmd);
                        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        String line = "";
                        lineNum = 0;
                        while((line = reader.readLine()) != null)
                        {
                                lineNum ++;
                                String rs = this.searchStr(line.substring(line.indexOf(':')+1), searchStr);
                                if(rs.length()>0)
                                {
                                        String linebyshell = line.substring(0, line.indexOf(':'));
                                        //System.out.println("Find in Line["+linebyshell+"], index: "+rs);
                                }
                        }
                        System.out.println("Finished!");
                        long endMili=System.currentTimeMillis();
                        System.out.println("Total times: "+(endMili-startMili)+" ms");
                        System.out.println("");
                }
                catch(Exception e)
                {
                        e.printStackTrace();
                }
        }
        public String searchStr(String src, String value)
        {
                String result = "";
                int index = src.indexOf(value,0);
                while(index>-1)
                {
                        result+=index+",";
                        index = src.indexOf(value,index+value.length());
                }
                return result;
        }
        public static boolean isNumeric(String str)
        {
            Pattern pattern = Pattern.compile("[0-9]*");
            return pattern.matcher(str).matches();
         }
        /**
         * @param args
         */
        public static void main(String[] args)
        {
                String file = "filenameFilter.xml";
                TransSearch test = new TransSearch();
                if(args.length>0)
                        test.setPath(args[0]);
                else
                        test.setPath(file);
                if(args.length>1)
                        test.setSearchStr(args[1]);
                else
                        test.setSearchStr("arrays2");
                test.start();
                test.startByShell();
        }
}
