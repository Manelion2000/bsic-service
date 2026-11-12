package com.bakouan.app.service;

import com.bakouan.app.model.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Mise en page commune aux actes : A4, identité, objet, visas et pagination. */
@Service @RequiredArgsConstructor
public class BaActePdfService {
    public static final float MAX_SIGNATURE_WIDTH_MM = 60;
    public static final float MAX_SIGNATURE_HEIGHT_MM = 20;
    private static final float POINTS_PER_MM = 72f / 25.4f;
    private static final Color NAVY = new Color(24, 50, 77), GOLD = new Color(177, 137, 51), MUTED = new Color(89, 103, 116), LIGHT = new Color(244, 247, 250);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Ouagadougou"));
    private final SignatureService signatures;

    public record Visa(String libelle, String nom, String fonction, String decision, String date, String commentaire, String delegation, BaActeSignature signature) {}
    public record Acte(String titre, String reference, String statut, Map<String, String> identite, Map<String, String> details, String objet, List<Visa> visas) {}

    public Map<String, String> identity(BaUser user) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Agent", CongeConfigurationService.name(user)); rows.put("Matricule", user.getMatricule()); rows.put("Fonction", label(Objects.toString(user.getFonction(), "")));
        rows.put("Département", user.getDepartement() != null ? user.getDepartement().getNom() : user.getService() != null && user.getService().getDepartement() != null ? user.getService().getDepartement().getNom() : "");
        rows.put("Service", user.getService() == null ? "" : user.getService().getNom());
        if (user.getAgence() != null) rows.put("Agence", user.getAgence().getNom());
        return rows;
    }

    public List<Visa> circuitVisas(BaRhCircuit circuit) {
        if (circuit == null) return List.of();
        List<BaActeSignature> copies = signatures.forAct("CIRCUIT", circuit.getId());
        return circuit.getHistorique().stream().filter(d -> Set.of("VALIDEE", "REJETEE").contains(d.decision())).map(d -> {
            BaActeSignature copy = copies.stream().filter(s -> s.getEtape() == d.etape() && s.getSignataireId().equals(d.idSignataire()) && "VALIDEE".equals(d.decision())).findFirst().orElse(null);
            String title = d.etape() < circuit.getEtapes().size() ? circuit.getEtapes().get(d.etape()).getLibelle() : label(d.type());
            return new Visa(title, copy == null ? d.nomSignataire() : copy.getNomSignataire(), copy == null ? d.fonctionSignataire() : copy.getFonctionSignataire(), d.decision(), copy == null ? d.date() : copy.getDateSignature().toString(), d.commentaire(), d.idDelegant() == null ? null : "Délégation " + d.idDelegation() + " ; titulaire " + d.idDelegant(), copy);
        }).toList();
    }

    public byte[] render(Acte acte) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42, 42, 36, 52);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document d) {
                    PdfContentByte canvas = w.getDirectContent();
                    canvas.setColorStroke(GOLD); canvas.setLineWidth(.6f); canvas.moveTo(d.left(), 39); canvas.lineTo(d.right(), 39); canvas.stroke();
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase("BSIC Burkina SA | Usage interne", font(8, Font.NORMAL, MUTED)), d.left(), 25, 0);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase("Page " + w.getPageNumber(), font(8, Font.NORMAL, MUTED)), d.right(), 25, 0);
                }
            });
            document.addTitle(acte.titre()); document.addAuthor("BSIC Burkina SA"); document.addSubject("Acte " + acte.reference()); document.open();
            PdfPTable header = new PdfPTable(new float[]{1.1f, 3.7f, 1}); header.setWidthPercentage(100);
            try (var logo = new ClassPathResource("image/bsic.png").getInputStream()) {
                Image image = Image.getInstance(logo.readAllBytes()); image.scaleToFit(70, 58);
                PdfPCell cell = plain(); cell.addElement(image); header.addCell(cell);
            }
            PdfPCell brand = plain(); brand.addElement(new Paragraph("BSIC BURKINA SA", font(14, Font.BOLD, NAVY))); brand.addElement(new Paragraph("Ressources humaines et démarches internes", font(9, Font.NORMAL, MUTED))); header.addCell(brand);
            ByteArrayOutputStream qr = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(new QRCodeWriter().encode("BSIC|REF=" + acte.reference() + "|ETAT=" + acte.statut(), BarcodeFormat.QR_CODE, 150, 150), "PNG", qr);
            Image qrImage = Image.getInstance(qr.toByteArray()); qrImage.scaleToFit(58, 58); PdfPCell qrCell = plain(); qrCell.addElement(qrImage); header.addCell(qrCell); document.add(header);
            Paragraph title = new Paragraph(acte.titre().toUpperCase(Locale.FRENCH), font(17, Font.BOLD, NAVY)); title.setSpacingBefore(12); title.setSpacingAfter(5); document.add(title);
            document.add(new Paragraph("Référence : " + acte.reference(), font(8, Font.NORMAL, MUTED)));
            Paragraph state = new Paragraph("STATUT : " + label(acte.statut()) + (Set.of("VALIDEE", "APPROUVEE").contains(acte.statut()) ? "" : " - DOCUMENT PROVISOIRE OU HISTORIQUE"), font(10, Font.BOLD, NAVY)); state.setSpacingBefore(8); state.setSpacingAfter(10); document.add(state);
            section(document, "IDENTITÉ DE L'AGENT"); fields(document, acte.identite());
            section(document, "OBJET ET PÉRIODE"); fields(document, acte.details());
            if (acte.objet() != null && !acte.objet().isBlank()) { Paragraph reason = new Paragraph(acte.objet(), font(10, Font.NORMAL, NAVY)); reason.setLeading(15); reason.setSpacingBefore(8); document.add(reason); }
            section(document, "VALIDATIONS ET SIGNATURES");
            if (acte.visas().isEmpty()) document.add(new Paragraph("Aucun visa enregistré pour ce document.", font(10, Font.NORMAL, MUTED)));
            for (Visa visa : acte.visas()) visa(document, visa);
            Paragraph note = new Paragraph("Les signatures manuscrites numérisées sont figées lors de l'approbation. Les décisions sont enregistrées dans le portail. Ce document ne comporte pas de signature électronique certifiée.", font(8, Font.NORMAL, MUTED)); note.setLeading(12); note.setSpacingBefore(14); document.add(note);
            document.close(); return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Génération de l'acte impossible.", e); }
        finally { if (document.isOpen()) document.close(); }
    }

    private void visa(Document document, Visa v) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{3, 2}); table.setWidthPercentage(100); table.setKeepTogether(true); table.setSpacingBefore(8);
        PdfPCell text = plain(); text.setPadding(10); text.setBackgroundColor(LIGHT);
        text.addElement(new Paragraph(Objects.toString(v.libelle(), "Visa"), font(10, Font.BOLD, NAVY)));
        text.addElement(new Paragraph(Objects.toString(v.nom(), "Identité non renseignée"), font(11, Font.BOLD, NAVY)));
        text.addElement(new Paragraph(label(v.fonction()), font(9, Font.NORMAL, MUTED)));
        text.addElement(new Paragraph(label(v.decision()) + " le " + formatInstant(v.date()), font(9, Font.NORMAL, NAVY)));
        if (v.commentaire() != null && !v.commentaire().isBlank()) text.addElement(new Paragraph(v.commentaire(), font(9, Font.NORMAL, MUTED)));
        if (v.delegation() != null) text.addElement(new Paragraph(v.delegation(), font(8, Font.NORMAL, MUTED)));
        table.addCell(text);
        PdfPCell signature = plain(); signature.setPadding(10); signature.setBackgroundColor(LIGHT); signature.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (v.signature() != null) {
            float availableWidth = (document.right() - document.left()) * 2f / 5f - signature.getPaddingLeft() - signature.getPaddingRight();
            Image image = signatureImage(v.signature().getContenu(), availableWidth);
            signature.addElement(image);
            Paragraph caption = new Paragraph("Signature du validateur", font(8, Font.NORMAL, MUTED)); caption.setAlignment(Element.ALIGN_CENTER); signature.addElement(caption);
            Paragraph hash = new Paragraph("Empreinte : " + v.signature().getEmpreinte().substring(0, 16), font(7, Font.NORMAL, MUTED)); hash.setAlignment(Element.ALIGN_CENTER); signature.addElement(hash);
        } else signature.addElement(new Paragraph(Set.of("VALIDEE", "VALIDE").contains(v.decision()) ? "Validation historique : signature non archivée." : "Aucune signature d'approbation.", font(9, Font.NORMAL, MUTED)));
        table.addCell(signature); document.add(table);
    }

    /** Taille physique bornée, proportions conservées, sans agrandir les petites images. */
    static Image signatureImage(byte[] content, float availableWidth) throws IOException, BadElementException {
        Image image = Image.getInstance(content);
        float widthLimit = Math.min(MAX_SIGNATURE_WIDTH_MM * POINTS_PER_MM, availableWidth);
        if (widthLimit <= 0) throw new IllegalArgumentException("La zone de signature est trop étroite.");
        float scale = Math.min(1f, Math.min(widthLimit / image.getWidth(), MAX_SIGNATURE_HEIGHT_MM * POINTS_PER_MM / image.getHeight()));
        image.scaleAbsolute(image.getWidth() * scale, image.getHeight() * scale);
        image.setAlignment(Element.ALIGN_CENTER);
        return image;
    }

    private void section(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, font(10, Font.BOLD, NAVY)); p.setSpacingBefore(10); p.setSpacingAfter(6); document.add(p);
    }
    private void fields(Document document, Map<String, String> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1, 1}); table.setWidthPercentage(100); table.setSplitLate(false);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER); table.getDefaultCell().setBackgroundColor(LIGHT);
        for (var entry : rows.entrySet()) {
            String value = entry.getValue() == null || entry.getValue().isBlank() ? "Non renseigné" : entry.getValue();
            Phrase phrase = new Phrase(); phrase.add(new Chunk(entry.getKey() + " : ", font(9, Font.BOLD, MUTED))); phrase.add(new Chunk(value, font(10, Font.NORMAL, NAVY)));
            PdfPCell cell = new PdfPCell(phrase); cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(7); cell.setLeading(13, 0); cell.setBackgroundColor(LIGHT);
            // Les champs longs ont toute la largeur et peuvent se poursuivre sur une autre page.
            if (value.length() > 85) { table.completeRow(); cell.setColspan(2); }
            table.addCell(cell);
        }
        table.completeRow();
        document.add(table);
    }
    private PdfPCell plain() { PdfPCell cell = new PdfPCell(); cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(0); return cell; }
    private Font font(float size, int style, Color color) { return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, size, style, color); }
    public static String date(LocalDate value) { return value == null ? "Non renseignée" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    public static String formatInstant(String value) { if (value == null || value.isBlank()) return "date non renseignée"; try { return DATE.format(Instant.parse(value)); } catch (java.time.format.DateTimeParseException e) { try { return DATE.format(ZonedDateTime.parse(value).toInstant()); } catch (java.time.format.DateTimeParseException ignored) { return value; } } }
    public static String label(String value) {
        if (value == null) return "";
        return switch (value) {
            case "VALIDEE", "VALIDE", "APPROUVEE" -> "Approuvé";
            case "REJETEE", "REJETE" -> "Refusé";
            case "EN_ATTENTE", "EN_COURS" -> "En attente";
            case "ANNULEE" -> "Annulé";
            case "CHEF_SERVICE" -> "Chef de service";
            case "AGENT" -> "Agent";
            case "DIRECTEUR" -> "Directeur";
            case "JOURNEE", "JOURNEE_ENTIERE" -> "Journée entière";
            case "MATIN" -> "Matin";
            case "APRES_MIDI" -> "Après-midi";
            case "DIRECTEUR_GENERAL_ADJOINT", "DGA" -> "Directeur général adjoint";
            case "DIRECTEUR_GENERAL", "DG" -> "Directeur général";
            default -> value.replace('_', ' ');
        };
    }
}
