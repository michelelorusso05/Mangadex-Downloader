package com.littleProgrammers.mangadexdownloader.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Arrays;

public class PDFHelper {
    String pdfName;
    String fileLocation;
    String pdfDestination;

    public PDFHelper() {
        // Horray...?
    }

    // Set the file name
    public void SetPDFName (String name) {
        pdfName = name;
    }

    // Set the source folder (the one with the images)
    public void SetSourcePath (String filePath)
    {
        fileLocation = filePath;
    }
    // Set destination folder
    public void SetDestinationPath (String filePath) { pdfDestination = filePath; }

    // Create a PDF given the parameters
    // Returns true if and only if the PDF was generated successfully
    public boolean CreatePDF() throws FileNotFoundException, MalformedURLException {
        // Initialize a new PDF document
        // Create a new PDF file at the specified path

        File targetDirectory = new File(pdfDestination);

        if (!targetDirectory.isDirectory()) {
            boolean success = targetDirectory.mkdirs();
            if (success) {
                Log.d("Debug", "Successfully created target folder");
            } else {
                Log.d("Debug", "Failed to create folder");
            }
        }

        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(targetDirectory + "/" + pdfName + ".pdf"));

        File[] cachedImages = GetFilesInPath(new File(fileLocation));

        if (cachedImages.length == 0) {
            SetPDFName("");
            return false;
        }

        // Load first image from disk
        ImageData imageData = ImageDataFactory.create(cachedImages[0].toString());
        // The procedure for the first page is a little bit different
        Image image = new Image(imageData);
        Document document = new Document(pdfDocument, new PageSize(image.getImageWidth(), image.getImageHeight()));

        document.setMargins(0, 0, 0, 0);
        document.add(image);

        for (int i = 1; i < cachedImages.length; i++)
        {
            // Load current image from disk
            imageData = ImageDataFactory.create(cachedImages[i].toString());

            image = new Image(imageData);
            document.add(new AreaBreak(new PageSize(image.getImageWidth(), image.getImageHeight())));
            document.add(image);
        }

        // Close the document
        document.close();

        return true;
    }

    // Returns the complete path to the last generated PDF
    public File getDownloadedPDFFilePath()
    {
        if (pdfName.equals(""))
            return null;
        return new File(pdfDestination + "/" + pdfName + ".pdf");
    }

    private File[] GetFilesInPath(@NonNull File dirPath)
    {
        File[] files = dirPath.listFiles();
        assert files != null;
        Arrays.sort(files);

        return files;
    }
}
