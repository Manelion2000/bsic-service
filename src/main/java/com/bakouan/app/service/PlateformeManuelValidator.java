package com.bakouan.app.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import static com.bakouan.app.service.CongeSecurity.bad;

@Component
public class PlateformeManuelValidator {
    public static final long MAX_FILE=10L*1024*1024, MAX_BATCH=20L*1024*1024;
    public static final String DOCX="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public record Fichier(String nom,String mime,String sha256,byte[] contenu) {}
    public Fichier validate(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>MAX_FILE)throw bad("Chaque manuel doit contenir entre 1 octet et 10 Mo.");
        String name=Objects.toString(file.getOriginalFilename(),"manuel").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","").trim();
        if(name.length()>150)name=name.substring(name.length()-150);
        String lower=name.toLowerCase(Locale.ROOT);
        try {
            byte[] bytes=file.getBytes();String mime;
            if(bytes.length==0||bytes.length>MAX_FILE)throw bad("Taille du manuel invalide.");
            if(lower.endsWith(".pdf")&&bytes.length>=8&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")
                &&new String(bytes,Math.max(0,bytes.length-2048),Math.min(bytes.length,2048),StandardCharsets.ISO_8859_1).contains("%%EOF"))mime="application/pdf";
            else if(lower.endsWith(".docx")&&validDocx(bytes))mime=DOCX;
            else throw bad("Manuel invalide : seuls les fichiers PDF et DOCX correspondant à leur contenu sont acceptés.");
            return new Fichier(name,mime,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),bytes);
        } catch(IOException e){throw bad("Le manuel ne peut pas être lu.");}
          catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private boolean validDocx(byte[] bytes) {
        if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K'||bytes[2]!=3||bytes[3]!=4)return false;
        byte[] types=null,document=null;long total=0;int count=0;var names=new HashSet<String>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                String name=entry.getName(),lower=name.toLowerCase(Locale.ROOT);
                if(++count>1000||!names.add(name)||name.startsWith("/")||name.contains("..")||name.contains("\\")
                    ||lower.contains("vbaproject")||lower.startsWith("word/activex/")||lower.startsWith("word/embeddings/"))return false;
                boolean capture=name.equals("[Content_Types].xml")||name.equals("word/document.xml");
                var out=capture?new ByteArrayOutputStream():null;byte[] buffer=new byte[8192];long part=0;
                for(int n;(n=zip.read(buffer))!=-1;) {
                    total+=n;part+=n;if(total>50L*1024*1024||(capture&&part>5L*1024*1024))return false;
                    if(capture)out.write(buffer,0,n);
                }
                if(name.equals("[Content_Types].xml"))types=out.toByteArray();
                if(name.equals("word/document.xml"))document=out.toByteArray();
            }
            if(types==null||document==null)return false;
            var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var builder=factory.newDocumentBuilder();builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            var contentTypes=builder.parse(new ByteArrayInputStream(types));boolean word=false;
            var overrides=contentTypes.getElementsByTagNameNS("http://schemas.openxmlformats.org/package/2006/content-types","Override");
            for(int i=0;i<overrides.getLength();i++) {
                var element=(org.w3c.dom.Element)overrides.item(i);
                if("/word/document.xml".equals(element.getAttribute("PartName"))&&"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml".equals(element.getAttribute("ContentType")))word=true;
            }
            var root=builder.parse(new ByteArrayInputStream(document)).getDocumentElement();
            return word&&"document".equals(root.getLocalName())&&Set.of("http://schemas.openxmlformats.org/wordprocessingml/2006/main","http://purl.oclc.org/ooxml/wordprocessingml/main").contains(root.getNamespaceURI());
        } catch(Exception e){return false;}
    }
}
