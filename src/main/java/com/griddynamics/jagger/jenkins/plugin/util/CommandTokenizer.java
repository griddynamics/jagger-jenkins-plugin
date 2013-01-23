package com.griddynamics.jagger.jenkins.plugin.util;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 1/22/13
 */
public class CommandTokenizer {

    public static String[] tokenize(String str) {

        ArrayList<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for(char c:str.toCharArray()) {
            if(c==' '){
                if(sb.length()>0){
                    list.add(sb.toString());
                    sb.setLength(0);
                }
                continue;
            } else if (c == ';') {
                list.add(sb.toString());
                list.add(";");
                sb.setLength(0);
                continue;
            }

            sb.append(c);
        }
        if(sb.length() > 0){
            list.add(sb.toString());
        }

        String [] array = new String[list.size()];
        for(int i = 0 ; i < list.size() ; i++){
            array[i] = list.get(i);
        }

        return array;
    }
}
