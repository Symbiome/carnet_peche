package fr.inrae.fishola.rest.feedback;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.entities.tables.pojos.UserFeedback;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Export des retours utilisateurs en CSV (séparateur « ; », UTF-8 avec BOM,
 * valeurs échappées) ou en classeur XLSX : le texte libre peut contenir
 * virgules, points-virgules, guillemets et sauts de ligne.
 */
public final class FeedbackExport {

    private FeedbackExport() {}

    private static final String CSV_SEPARATOR = ";";
    private static final String UTF8_BOM = "﻿";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";
    private static final int MIN_COLUMN_CHARS = 12;
    private static final int MAX_COLUMN_CHARS = 80;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private record Column(String header, Function<UserFeedback, String> value) {}

    private static final List<Column> COLUMNS = List.of(
            new Column("date_reception", feedback -> DATE_FORMAT.format(feedback.getReceivedOn())),
            new Column("categorie", UserFeedback::getCategory),
            new Column("origine", UserFeedback::getSource),
            new Column("module", UserFeedback::getModule),
            new Column("email", UserFeedback::getEmail),
            new Column("description", UserFeedback::getDescription),
            new Column("capture_ecran", feedback -> Boolean.TRUE.equals(feedback.getHasScreenshot()) ? "oui" : "non"),
            new Column("page", UserFeedback::getLocation),
            new Column("version_frontend", UserFeedback::getFrontendVersion),
            new Column("version_backend", UserFeedback::getBackendVersion),
            new Column("navigateur", UserFeedback::getBrowser),
            new Column("os", UserFeedback::getOs),
            new Column("plateforme", UserFeedback::getPlatform),
            new Column("appareil", UserFeedback::getDevice),
            new Column("resolution_ecran", UserFeedback::getScreenResolution),
            new Column("taille_affichage", UserFeedback::getDisplaySize),
            new Column("langue", UserFeedback::getLocale));

    /**
     * Construit l'export CSV.
     *
     * @param feedbacks retours à exporter, dans l'ordre voulu
     * @return le contenu CSV, BOM compris pour qu'Excel lise l'UTF-8
     */
    public static String toCsv(List<UserFeedback> feedbacks) {
        Stream<String> header = Stream.of(COLUMNS.stream().map(Column::header)
                .collect(Collectors.joining(CSV_SEPARATOR)));
        Stream<String> lines = feedbacks.stream().map(FeedbackExport::toCsvLine);
        return UTF8_BOM + Stream.concat(header, lines).collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    /**
     * Construit l'export XLSX : une feuille, en-têtes en gras et figés, texte
     * stocké en cellules de type chaîne (jamais interprété comme formule).
     *
     * @param feedbacks retours à exporter, dans l'ordre voulu
     * @return le classeur sérialisé
     */
    public static byte[] toXlsx(List<UserFeedback> feedbacks) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Retours utilisateurs");
            writeHeader(sheet, boldStyle(workbook));
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            for (int r = 0; r < feedbacks.size(); r++) {
                writeRow(sheet.createRow(r + 1), feedbacks.get(r), wrapStyle);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, feedbacks.size(), 0, COLUMNS.size() - 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toCsvLine(UserFeedback feedback) {
        return COLUMNS.stream()
                .map(column -> csvCell(StringUtils.defaultString(column.value().apply(feedback))))
                .collect(Collectors.joining(CSV_SEPARATOR));
    }

    /**
     * Neutralise une valeur commençant comme une formule tableur, puis la met entre
     * guillemets si elle contient le séparateur, une virgule, un guillemet ou un saut de ligne.
     */
    static String csvCell(String value) {
        String safe = !value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (StringUtils.containsAny(safe, CSV_SEPARATOR, ",", "\"", "\n", "\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static CellStyle boldStyle(XSSFWorkbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    private static void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int c = 0; c < COLUMNS.size(); c++) {
            row.createCell(c).setCellValue(COLUMNS.get(c).header());
            row.getCell(c).setCellStyle(headerStyle);
            int chars = "description".equals(COLUMNS.get(c).header()) ? MAX_COLUMN_CHARS : MIN_COLUMN_CHARS;
            sheet.setColumnWidth(c, Math.max(chars, COLUMNS.get(c).header().length() + 2) * 256);
        }
    }

    private static void writeRow(Row row, UserFeedback feedback, CellStyle wrapStyle) {
        for (int c = 0; c < COLUMNS.size(); c++) {
            String value = COLUMNS.get(c).value().apply(feedback);
            if (value != null) {
                row.createCell(c).setCellValue(value);
                row.getCell(c).setCellStyle(wrapStyle);
            }
        }
    }
}
