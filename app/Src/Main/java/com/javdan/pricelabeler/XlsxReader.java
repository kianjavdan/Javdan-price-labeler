package com.javdan.pricelabeler;

import android.content.*;
import android.net.Uri;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class XlsxReader {
    private final Context context;
    public XlsxReader(Context c){ context=c; }

    public ArrayList<LinkedHashMap<String,String>> readFirstSheet(Uri uri) throws Exception {
        File temp = new File(context.getCacheDir(),"javdan_excel_"+System.currentTimeMillis()+".xlsx");
        try(InputStream in=context.getContentResolver().openInputStream(uri); OutputStream out=new FileOutputStream(temp)){
            byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n);
        }
        try(ZipFile z=new ZipFile(temp)){
            ArrayList<String> shared=readShared(z);
            ZipEntry sheet=z.getEntry("xl/worksheets/sheet1.xml");
            if(sheet==null) throw new IOException("Sheet اول پیدا نشد.");
            Document d=parse(z.getInputStream(sheet));
            NodeList rows=d.getElementsByTagName("row");
            ArrayList<ArrayList<String>> matrix=new ArrayList<>();
            for(int i=0;i<rows.getLength();i++){
                Element row=(Element)rows.item(i);
                NodeList cells=row.getElementsByTagName("c");
                ArrayList<String> vals=new ArrayList<>();
                int expected=0;
                for(int j=0;j<cells.getLength();j++){
                    Element c=(Element)cells.item(j);
                    String ref=c.getAttribute("r");
                    int col=colIndex(ref);
                    while(expected<col){ vals.add(""); expected++; }
                    String type=c.getAttribute("t");
                    String value="";
                    NodeList vs=c.getElementsByTagName("v");
                    if(vs.getLength()>0) value=vs.item(0).getTextContent();
                    if("s".equals(type) && !value.isEmpty()){
                        int idx=Integer.parseInt(value);
                        value=(idx>=0 && idx<shared.size())?shared.get(idx):"";
                    } else if("inlineStr".equals(type)){
                        NodeList ts=c.getElementsByTagName("t");
                        value=ts.getLength()>0?ts.item(0).getTextContent():"";
                    }
                    vals.add(value); expected++;
                }
                matrix.add(vals);
            }
            if(matrix.isEmpty()) return new ArrayList<>();
            ArrayList<String> headers=matrix.get(0);
            ArrayList<LinkedHashMap<String,String>> out=new ArrayList<>();
            for(int i=1;i<matrix.size();i++){
                LinkedHashMap<String,String> m=new LinkedHashMap<>();
                ArrayList<String> row=matrix.get(i);
                for(int j=0;j<headers.size();j++){
                    String h=headers.get(j);
                    if(h==null||h.trim().isEmpty()) h="Column_"+(j+1);
                    m.put(h,j<row.size()?row.get(j):"");
                }
                out.add(m);
            }
            return out;
        } finally { temp.delete(); }
    }

    private ArrayList<String> readShared(ZipFile z) throws Exception{
        ArrayList<String> out=new ArrayList<>();
        ZipEntry e=z.getEntry("xl/sharedStrings.xml");
        if(e==null) return out;
        Document d=parse(z.getInputStream(e));
        NodeList sis=d.getElementsByTagName("si");
        for(int i=0;i<sis.getLength();i++){
            Element si=(Element)sis.item(i);
            NodeList ts=si.getElementsByTagName("t");
            StringBuilder s=new StringBuilder();
            for(int j=0;j<ts.getLength();j++) s.append(ts.item(j).getTextContent());
            out.add(s.toString());
        }
        return out;
    }

    private Document parse(InputStream in) throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setExpandEntityReferences(false);
        DocumentBuilder b=f.newDocumentBuilder();
        return b.parse(in);
    }

    private int colIndex(String ref){
        int n=0;
        for(int i=0;i<ref.length();i++){
            char ch=ref.charAt(i);
            if(ch>='A'&&ch<='Z') n=n*26+(ch-'A'+1); else break;
        }
        return Math.max(0,n-1);
    }
}
