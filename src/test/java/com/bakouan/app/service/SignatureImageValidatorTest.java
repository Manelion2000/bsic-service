package com.bakouan.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.assertj.core.api.Assertions.*;

class SignatureImageValidatorTest {
    private final SignatureImageValidator validator = new SignatureImageValidator();
    @ParameterizedTest @ValueSource(strings={"PNG","JPEG","BMP","GIF","TIFF"})
    void normalizesTheRealFileFormatRegardlessOfTheExtensionOrDeclaredMimeType(String format) throws Exception {
        BufferedImage source=signature(220,80);ByteArrayOutputStream out=new ByteArrayOutputStream();
        assertThat(ImageIO.write(source,format,out)).isTrue();
        BufferedImage result=ImageIO.read(new ByteArrayInputStream(validator.normalize(new MockMultipartFile("file","wrong.bin","application/octet-stream",out.toByteArray()))));
        assertThat(result.getColorModel().hasAlpha()).isTrue();assertThat(result.getWidth()).isLessThanOrEqualTo(900);assertThat(result.getHeight()).isLessThanOrEqualTo(300);assertThat(result.getRGB(0,0)>>>24).isZero();
    }
    @Test void normalizesARealWebpImage() throws Exception {
        byte[] bytes;
        try(var resource=getClass().getResourceAsStream("/signatures/blue-tile.webp")){assertThat(resource).isNotNull();bytes=resource.readAllBytes();}
        byte[] png=validator.normalize(new MockMultipartFile("file","s.webp","image/webp",bytes));
        assertThat(png).startsWith(new byte[]{(byte)137,80,78,71,13,10,26,10});assertThat(ImageIO.read(new ByteArrayInputStream(png)).getWidth()).isGreaterThanOrEqualTo(20);
    }
    @ParameterizedTest @ValueSource(strings={"GIF","TIFF"})
    void rejectsAnimatedOrMultipageFiles(String format) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();var writer=ImageIO.getImageWritersByFormatName(format).next();
        try(var imageOut=ImageIO.createImageOutputStream(out)){
            writer.setOutput(imageOut);writer.prepareWriteSequence(null);
            writer.writeToSequence(new javax.imageio.IIOImage(signature(220,80),null,null),null);
            writer.writeToSequence(new javax.imageio.IIOImage(signature(220,80),null,null),null);writer.endWriteSequence();
        }finally{writer.dispose();}
        assertThatThrownBy(()->validator.normalize(new MockMultipartFile("file","s."+format,"image/"+format,out.toByteArray()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("seule image fixe");
    }
    @Test void refusesAnAnimatedPngEvenWhenTheReaderOnlySeesItsDefaultImage() throws Exception {
        ByteArrayOutputStream original=new ByteArrayOutputStream();ImageIO.write(signature(220,80),"PNG",original);byte[] png=original.toByteArray();
        ByteArrayOutputStream modified=new ByteArrayOutputStream();modified.write(png,0,33);
        var data=new DataOutputStream(modified);data.writeInt(8);byte[] chunk=new byte[]{'a','c','T','L',0,0,0,2,0,0,0,0};data.write(chunk);java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(chunk);data.writeInt((int)crc.getValue());modified.write(png,33,png.length-33);
        assertThatThrownBy(()->validator.normalize(new MockMultipartFile("file","s.png","image/png",modified.toByteArray()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("animations");
    }
    @Test void rejectsTheAnimationFlagInAWebpContainerBeforeDecodingItsPixels() throws Exception {
        byte[] original;try(var resource=getClass().getResourceAsStream("/signatures/blue-tile.webp")){original=resource.readAllBytes();}
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{'R','I','F','F'});int length=original.length+10;for(int i=0;i<4;i++)out.write(length>>>(8*i));
        out.write(new byte[]{'W','E','B','P','V','P','8','X',10,0,0,0,2,0,0,0,31,0,0,31,0,0});out.write(original,12,original.length-12);
        assertThatThrownBy(()->validator.normalize(new MockMultipartFile("file","s.webp","image/webp",out.toByteArray()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("animations");
    }
    @ParameterizedTest @ValueSource(strings={"900x20","20x300","2000x200","200x2000"})
    void fitsWideAndTallSignaturesWithoutDistortionOrUpscaling(String dimensions) throws Exception {
        String[] parts=dimensions.split("x");int width=Integer.parseInt(parts[0]),height=Integer.parseInt(parts[1]);
        BufferedImage source=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=source.createGraphics();g.setColor(Color.BLUE);g.fillRect(0,0,width,height);g.dispose();
        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(source,"PNG",out);BufferedImage result=ImageIO.read(new ByteArrayInputStream(validator.normalize(new MockMultipartFile("file","s.png","image/png",out.toByteArray()))));
        assertThat(result.getWidth()).isLessThanOrEqualTo(Math.min(width,900));assertThat(result.getHeight()).isLessThanOrEqualTo(Math.min(height,300));
        double scale=Math.min(1d,Math.min(900d/width,300d/height));assertThat((double)result.getWidth()).isCloseTo(width*scale,within(1d));assertThat((double)result.getHeight()).isCloseTo(height*scale,within(1d));
    }
    @Test void rejectsInsufficientDimensionsAndExcessivePixelCount() throws Exception {
        for(int[] size:new int[][]{{19,50},{50,9},{3500,3500}}){ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(signature(size[0],size[1]),"PNG",out);assertThatThrownBy(()->validator.normalize(new MockMultipartFile("file","s.png","image/png",out.toByteArray()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Dimensions invalides");}
    }
    private BufferedImage signature(int width,int height){BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,width,height);g.setColor(Color.BLUE);g.setStroke(new BasicStroke(3));g.drawLine(width/4,height/3,width*3/4,height*2/3);g.dispose();return image;}
    @Test void cropsWhitespaceKeepsTransparencyAndBoundsTheStoredImage() throws Exception {
        BufferedImage photo = new BufferedImage(1800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = photo.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, 1800, 800); g.setColor(Color.BLUE); g.setStroke(new BasicStroke(8)); g.drawLine(400, 400, 1400, 500); g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(photo, "JPEG", out);
        byte[] png = validator.normalize(new MockMultipartFile("file", "scan.jpg", "application/octet-stream", out.toByteArray()));
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result.getWidth()).isLessThanOrEqualTo(900); assertThat(result.getHeight()).isLessThanOrEqualTo(300); assertThat(result.getRGB(0, 0) >>> 24).isZero();
        assertThat(png).startsWith(new byte[]{(byte)137,80,78,71,13,10,26,10});
    }
    @Test void rejectsScriptDisguisedAsPngAndOversizedPayload() {
        assertThatThrownBy(() -> validator.normalize(new MockMultipartFile("file", "signature.png", "image/png", "<svg onload='alert(1)'/>".getBytes()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> validator.normalize(new MockMultipartFile("file", "signature.png", "image/png", new byte[SignatureImageValidator.MAX_BYTES + 1]))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsBlankImageAndHugeDecodedDimensions() throws Exception {
        for (int size : new int[]{100, 4100}) {
            BufferedImage image = new BufferedImage(size, 30, BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, size, 30); g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(image, "PNG", out);
            assertThatThrownBy(() -> validator.normalize(new MockMultipartFile("file", "s.png", "image/png", out.toByteArray()))).isInstanceOf(ResponseStatusException.class);
        }
    }
}
