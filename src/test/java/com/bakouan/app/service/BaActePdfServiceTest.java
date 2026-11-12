package com.bakouan.app.service;

import com.bakouan.app.model.BaActeSignature;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.cos.COSName;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaActePdfServiceTest {
    BaActePdfService service = new BaActePdfService(mock(SignatureService.class));
    @Test void exportedPdfBoundsWideTallAndSmallSignaturesInMillimetresAndPreservesTheirAspectRatio() throws Exception {
        var base=acte("Contrôle des dimensions des signatures",0,false);List<BaActePdfService.Visa> visas=new ArrayList<>();
        for(int[] size:new int[][]{{900,20},{20,300},{40,15}}){
            BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(Color.BLUE);g.fillRect(0,0,size[0],size[1]);g.dispose();ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"PNG",out);
            BaActeSignature signature=new BaActeSignature();signature.setContenu(out.toByteArray());signature.setEmpreinte("a".repeat(64));
            visas.add(new BaActePdfService.Visa("Dimensions "+size[0]+" × "+size[1],"Validateur","DIRECTEUR","VALIDEE",Instant.now().toString(),null,null,signature));
        }
        byte[] bytes=service.render(new BaActePdfService.Acte(base.titre(),base.reference(),base.statut(),base.identite(),base.details(),base.objet(),visas));
        int[] count={0};
        try(var pdf=Loader.loadPDF(bytes)){
            for(var page:pdf.getPages())new SignatureDimensions(page,count).processPage(page);
            assertThat(count[0]).isEqualTo(3);
            if(Boolean.getBoolean("bsic.acte.preview")){
                Path dir=Path.of("target/actes-preview");Files.createDirectories(dir);Files.write(dir.resolve("dimensions-signatures.pdf"),bytes);PDFRenderer renderer=new PDFRenderer(pdf);for(int page=0;page<pdf.getNumberOfPages();page++)ImageIO.write(renderer.renderImageWithDPI(page,110),"PNG",dir.resolve("dimensions-signatures-"+(page+1)+".png").toFile());
            }
        }
        var small=BaActePdfService.signatureImage(visas.get(2).signature().getContenu(),185);
        assertThat(small.getScaledWidth()).isEqualTo(40);assertThat(small.getScaledHeight()).isEqualTo(15);
        var narrow=BaActePdfService.signatureImage(visas.get(0).signature().getContenu(),70);
        assertThat(narrow.getScaledWidth()).isCloseTo(70,within(.01f));
    }
    private static class SignatureDimensions extends PDFGraphicsStreamEngine {
        final int[] count;Point2D current=new Point2D.Float();
        SignatureDimensions(PDPage page,int[] count){super(page);this.count=count;}
        @Override public void drawImage(PDImage image){
            if(!((image.getWidth()==900&&image.getHeight()==20)||(image.getWidth()==20&&image.getHeight()==300)||(image.getWidth()==40&&image.getHeight()==15)))return;
            count[0]++;var m=getGraphicsState().getCurrentTransformationMatrix();float width=m.getScalingFactorX(),height=m.getScalingFactorY();
            assertThat(width).isLessThanOrEqualTo(60*72f/25.4f+.02f);assertThat(height).isLessThanOrEqualTo(20*72f/25.4f+.02f);
            assertThat(width/height).isCloseTo((float)image.getWidth()/image.getHeight(),within(.01f));
            assertThat(width).isLessThanOrEqualTo(image.getWidth()+.02f);assertThat(height).isLessThanOrEqualTo(image.getHeight()+.02f);
            assertThat(m.getTranslateX()).isGreaterThanOrEqualTo(42);assertThat(m.getTranslateX()+width).isLessThanOrEqualTo(getPage().getMediaBox().getWidth()-42+.02f);
            assertThat(m.getTranslateY()).isGreaterThanOrEqualTo(52);assertThat(m.getTranslateY()+height).isLessThanOrEqualTo(getPage().getMediaBox().getHeight()-36+.02f);
        }
        @Override public void appendRectangle(Point2D a,Point2D b,Point2D c,Point2D d){}
        @Override public void clip(int rule){}
        @Override public void moveTo(float x,float y){current=new Point2D.Float(x,y);}
        @Override public void lineTo(float x,float y){current=new Point2D.Float(x,y);}
        @Override public void curveTo(float a,float b,float c,float d,float x,float y){current=new Point2D.Float(x,y);}
        @Override public Point2D getCurrentPoint(){return current;}
        @Override public void closePath(){}
        @Override public void endPath(){}
        @Override public void strokePath(){}
        @Override public void fillPath(int rule){}
        @Override public void fillAndStrokePath(int rule){}
        @Override public void shadingFill(COSName name){}
    }
    @Test void allActFamiliesRenderReadableFrenchTextAndArchivedSignatureImages() throws Exception {
        for (String title : List.of("Autorisation de congé", "Autorisation d'absence", "Autorisation de sortie", "Attestation de reprise de service", "Fiche d'habilitation", "Acte de validation")) {
            byte[] pdf = service.render(acte(title, 3, false));
            try (var doc = Loader.loadPDF(pdf)) {
                String text = new PDFTextStripper().getText(doc);
                assertThat(text).contains(title.toUpperCase(Locale.FRENCH), "Ouédraogo Aminata", "Signature du validateur", "Page 1");
                assertThat(doc.getNumberOfPages()).isBetween(1, 3);
                long images = 0;
                for (var page : doc.getPages()) for (var name : page.getResources().getXObjectNames()) if (page.getResources().isImageXObject(name)) images++;
                assertThat(images).isGreaterThanOrEqualTo(5); // Logo, QR et les trois signatures.
                if (Boolean.getBoolean("bsic.acte.preview")) {
                    Path folder = Path.of("target/actes-preview"); Files.createDirectories(folder);
                    String file = title.replaceAll("[^a-zA-Z]", "_"); Files.write(folder.resolve(file + ".pdf"), pdf);
                    PDFRenderer renderer = new PDFRenderer(doc);
                    for (int page = 0; page < doc.getNumberOfPages(); page++) ImageIO.write(renderer.renderImageWithDPI(page, 110), "PNG", folder.resolve(file + "-" + (page + 1) + ".png").toFile());
                }
            }
        }
    }
    @Test void longCommentsPaginateWithoutLosingVisasAndHistoricalDecisionsAreExplicit() throws Exception {
        byte[] pdf = service.render(acte("Autorisation de congé", 6, true));
        try (var doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Chef 5", "Validation historique : signature non archivée.", "FIN DU COMMENTAIRE"); assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }
    private BaActePdfService.Acte acte(String title, int count, boolean historic) throws Exception {
        Map<String,String> identity = new LinkedHashMap<>(); identity.put("Agent", "Ouédraogo Aminata"); identity.put("Matricule", "BSIC-0042"); identity.put("Fonction", "Agent"); identity.put("Département", "Opérations bancaires"); identity.put("Service", "Comptabilité et rapprochements");
        Map<String,String> details = new LinkedHashMap<>(); details.put("Période autorisée", "Du 19/10/2026 au 23/10/2026"); details.put("Durée", "5 jours ouvrés"); details.put("Décision finale", "14/10/2026 10:35");
        List<BaActePdfService.Visa> visas = new ArrayList<>();
        for (int i=0;i<count;i++) {
            BaActeSignature s = new BaActeSignature(); s.setContenu(SignatureTestImages.png(i)); s.setEmpreinte("abcdef0123456789".repeat(4));
            visas.add(new BaActePdfService.Visa("Chef " + i, "Koné Moussa " + i, "DIRECTEUR", "VALIDEE", Instant.now().toString(), historic ? "Commentaire de validation avec des précisions sur la continuité du service. ".repeat(15) + " FIN DU COMMENTAIRE" : "Avis favorable. Continuité de service assurée.", null, historic ? null : s));
        }
        return new BaActePdfService.Acte(title, "BSIC-2026-0042", "VALIDEE", identity, details, "L'absence de l'agent est autorisée pour la période indiquée. La continuité des activités est assurée par le service.", visas);
    }
}
