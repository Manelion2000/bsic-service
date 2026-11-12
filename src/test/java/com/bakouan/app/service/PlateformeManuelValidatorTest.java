package com.bakouan.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class PlateformeManuelValidatorTest {
    private final PlateformeManuelValidator validator=new PlateformeManuelValidator();
    static byte[] pdf(String text){return ("%PDF-1.7\n"+text+"\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);}
    static byte[] docx(boolean macro,boolean entity) throws IOException {
        var out=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(out)) {
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(zip,"word/document.xml",(entity?"<!DOCTYPE document [<!ENTITY x SYSTEM 'file:///C:/secret'>]>":"")+"<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body/></w:document>");
            if(macro)entry(zip,"word/vbaProject.bin","macro");
        }
        return out.toByteArray();
    }
    private static void entry(ZipOutputStream zip,String name,String content)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    @Test void detectsTheContentAndSanitizesTheFilenameInsteadOfTrustingTheClientMime(){
        var validated=validator.validate(new MockMultipartFile("fichiers","C:\\fakepath\\manuel\r\n.pdf","text/html",pdf("Guide")));
        assertThat(validated.nom()).isEqualTo("manuel.pdf");assertThat(validated.mime()).isEqualTo("application/pdf");assertThat(validated.sha256()).hasSize(64);
    }
    @Test void rejectsEmptyOversizedOrDisguisedFiles(){
        for(var file:new MockMultipartFile[]{new MockMultipartFile("fichiers","empty.pdf","application/pdf",new byte[0]),
            new MockMultipartFile("fichiers","fake.pdf","application/pdf","<html>fake</html>".getBytes()),
            new MockMultipartFile("fichiers","script.html","application/pdf",pdf("X")),
            new MockMultipartFile("fichiers","large.pdf","application/pdf",new byte[(int)PlateformeManuelValidator.MAX_FILE+1])})
            assertThatThrownBy(()->validator.validate(file)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void acceptsDocxButRejectsMacrosAndExternalXmlEntities()throws Exception {
        assertThat(validator.validate(new MockMultipartFile("fichiers","guide.docx","application/octet-stream",docx(false,false))).mime()).isEqualTo(PlateformeManuelValidator.DOCX);
        for(byte[] bytes:new byte[][]{docx(true,false),docx(false,true)})
            assertThatThrownBy(()->validator.validate(new MockMultipartFile("fichiers","guide.docx","application/zip",bytes))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsZipTraversalAndZipThatIsNotAWordDocument()throws Exception {
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){entry(zip,"../outside.xml","Data");}
        assertThatThrownBy(()->validator.validate(new MockMultipartFile("fichiers","guide.docx","application/zip",out.toByteArray()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->validator.validate(new MockMultipartFile("fichiers","guide.docx","application/zip",pdf("fakezip")))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void boundsZipExpansionBeforeParsingXml()throws Exception {
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write(new byte[6*1024*1024]);zip.closeEntry();}
        assertThatThrownBy(()->validator.validate(new MockMultipartFile("fichiers","guide.docx","application/zip",out.toByteArray()))).isInstanceOf(ResponseStatusException.class);
    }
}
