package com.littleProgrammers.mangadexdownloader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PDFHelper {
    public static File GeneratePDF(File targetDirectory, File sourceDir, String pdfName)
    {
        return GeneratePDF(targetDirectory, sourceDir, pdfName, null);
    }
    @Nullable
    public static File GeneratePDF(@NonNull File targetDirectory, File sourceDir, String pdfName, Consumer<Float> progressCallback)
    {
        if (!targetDirectory.isDirectory()) {
            boolean success = targetDirectory.mkdirs();
            if (success) {
                Log.d("Debug", "Successfully created target folder");
            } else {
                Log.d("Debug", "Failed to create folder");
            }
        }

        File file = new File(targetDirectory, pdfName + ".pdf");
        File[] images = FolderUtilities.GetOrderedFilesInPath(sourceDir);

        if (images.length == 0) return null;

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            PdfDocument pdfDocument = new PdfDocument();

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inMutable = false;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            opt.inSampleSize = 2;

            for (int i = 0; i < images.length; i++) {
                Bitmap bitmap = BitmapFactory.decodeFile(images[i].getPath(), opt);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), (i + 1)).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawBitmap(bitmap, 0f, 0f, null);
                pdfDocument.finishPage(page);
                bitmap.recycle();

                if (progressCallback != null)
                    progressCallback.accept((((float) (i + 1)) / images.length) * 100f);
            }

            if (progressCallback != null)
                progressCallback.accept(-1f);

            pdfDocument.writeTo(fileOutputStream);
            pdfDocument.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }
}
