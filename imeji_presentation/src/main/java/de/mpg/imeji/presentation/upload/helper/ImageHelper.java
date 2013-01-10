/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.upload.helper;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.PNGDecodeParam;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

import de.mpg.imeji.presentation.util.PropertyReader;

//import org.ajax4jsf.resource.image.animatedgif.GifDecoder;
/**
 * Mehtods to help wotk with images
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImageHelper
{
    /**
     * Scale a {@link BufferedImage} to new size. Quality of the images is high. Is slow and resource consuming
     * 
     * @param image
     * @param size
     * @param resolution
     * @return
     * @throws Exception
     */
    public static BufferedImage scaleImage(BufferedImage image, int size, String resolution) throws Exception
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage newImg = null;
        Image rescaledImage;
        if (width > height)
        {
            if (resolution.equals(getThumb()))
            {
                newImg = new BufferedImage(height, height, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, (height - width) / 2, 0, null);
                if (height > size)
                    rescaledImage = newImg.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
        }
        else
        {
            if (resolution.equals(getThumb()))
            {
                newImg = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, 0, (width - height) / 2, null);
                if (width > size)
                    rescaledImage = getScaledInstance(newImg, width, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            true);
                else
                    rescaledImage = newImg;
                if (width > size)
                    rescaledImage = newImg.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
        }
        BufferedImage rescaledBufferedImage = new BufferedImage(rescaledImage.getWidth(null),
                rescaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g2 = rescaledBufferedImage.getGraphics();
        g2.drawImage(rescaledImage, 0, 0, null);
        return rescaledBufferedImage;
    }

    /**
     * Scale a {@link BufferedImage} to new size. Is faster than the basic {@link ImageHelper}.scaleImage method, has
     * the same quality
     * 
     * @param image original image
     * @param size the size to be resized to
     * @param resolution the type of the image. Might be thumb or web
     * @return the resized images
     * @throws Exception
     */
    public static BufferedImage scaleImageFast(BufferedImage image, int size, String resolution) throws Exception
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage newImg = null;
        Image rescaledImage;
        if (width > height)
        {
            if (resolution.equals(getThumb()))
            {
                newImg = new BufferedImage(height, height, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, (height - width) / 2, 0, null);
                if (height > size)
                    rescaledImage = getScaledInstance(newImg, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            true);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = getScaledInstance(image, size, height * size / width,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
        }
        else
        {
            if (resolution.equals(getThumb()))
            {
                newImg = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
                Graphics g1 = newImg.createGraphics();
                g1.drawImage(image, 0, (width - height) / 2, null);
                if (width > size)
                    rescaledImage = getScaledInstance(newImg, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            true);
                else
                    rescaledImage = newImg;
            }
            else
                rescaledImage = getScaledInstance(image, width * size / height, size,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
        }
        BufferedImage rescaledBufferedImage = new BufferedImage(rescaledImage.getWidth(null),
                rescaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g2 = rescaledBufferedImage.getGraphics();
        g2.drawImage(rescaledImage, 0, 0, null);
        return rescaledBufferedImage;
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@link BufferedImage}.
     * 
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling technique that provides higher quality
     *            than the usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
     *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
     *            {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@link BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality)
        {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        }
        else
        {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }
        do
        {
            if (higherQuality && w > targetWidth)
            {
                w /= 2;
                if (w < targetWidth)
                {
                    w = targetWidth;
                }
            }
            if (higherQuality && h > targetHeight)
            {
                h /= 2;
                if (h < targetHeight)
                {
                    h = targetHeight;
                }
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        }
        while (w != targetWidth || h != targetHeight);
        return ret;
    }

    /**
     * Transform a tiff image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] tiff2Jpeg(byte[] bytes)
    {
        try
        {
            File tiffFile = File.createTempFile("upload", "tif.tmp");
            FileUtils.writeByteArrayToFile(tiffFile, bytes);
            SeekableStream s = new FileSeekableStream(tiffFile);
            TIFFDecodeParam param = new TIFFDecodeParam();
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
            return image2Jpeg(tiffFile, dec);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming tiff to jpeg", e);
        }
    }

    /**
     * Transform a png image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] png2Jpeg(byte[] bytes)
    {
        try
        {
            File pngFile = File.createTempFile("upload", "png.tmp");
            FileUtils.writeByteArrayToFile(pngFile, bytes);
            SeekableStream s = new FileSeekableStream(pngFile);
            PNGDecodeParam param = new PNGDecodeParam();
            ImageDecoder dec = ImageCodec.createImageDecoder("png", s, param);
            return image2Jpeg(pngFile, dec);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming png to jpeg", e);
        }
    }

    /**
     * Transform a bmp image into a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] bmp2Jpeg(byte[] bytes)
    {
        return image2Jpeg(bytes);
    }

    /**
     * Transform a gif image to a jpeg image
     * 
     * @param bytes
     * @return
     */
    public static byte[] gif2Jpeg(byte[] bytes)
    {
        return image2Jpeg(bytes);
    }

    /**
     * Transform a image to a jpeg image. The input image must have a format supported by {@link ImageIO}
     * 
     * @param bytes
     * @return
     */
    private static byte[] image2Jpeg(byte[] bytes)
    {
        try
        {
            InputStream ins = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(ins);
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", ous);
            return ous.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming image to jpeg", e);
        }
    }

    /**
     * Transform a image {@link File} to a jpeg file
     * 
     * @param f - the {@link File} where the image is
     * @param dec - The {@link ImageDecoder} needed to decode the passed image
     * @return the image as {@link Byte} array
     */
    private static byte[] image2Jpeg(File f, ImageDecoder dec)
    {
        try
        {
            RenderedImage ri = dec.decodeAsRenderedImage(0);
            File jpgFile = File.createTempFile("imeji_upload", "jpg.tmp");
            FileOutputStream fos = new FileOutputStream(jpgFile);
            JPEGEncodeParam jParam = new JPEGEncodeParam();
            jParam.setQuality(1.0f);
            ImageEncoder imageEncoder = ImageCodec.createImageEncoder("JPEG", fos, jParam);
            imageEncoder.encode(ri);
            fos.flush();
            return FileUtils.readFileToByteArray(jpgFile);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error transforming image file to jpeg", e);
        }
    }

    /**
     * Return the format of an image (jpg, tif), according to its mime-type
     * 
     * @param mimeType
     * @return
     */
    public static String getImageFormat(String mimeType)
    {
        if (mimeType.equals(getMimeType("tif")))
        {
            return "tif";
        }
        return mimeType.toLowerCase().replaceAll("image/", "");
    }

    /**
     * Return the Mime Type of an image according to its format (i.e. file extension)
     * 
     * @param format
     * @return
     */
    public static String getMimeType(String format)
    {
        format = format.toLowerCase();
        if ("tif".equals(format))
        {
            format = format + "f";
        }
        return "image/" + format;
    }

    // public static GifDecoder checkAnimation(byte[] image) throws Exception
    // {
    // GifDecoder gifDecoder = new GifDecoder();
    // gifDecoder.read(new ByteArrayInputStream(image));
    // return gifDecoder;
    // }
    //
    // public static byte[] scaleAnimation(byte[] image, GifDecoder gifDecoder, int width) throws Exception
    // {
    // ByteArrayOutputStream outputStream =new ByteArrayOutputStream();
    // outputStream.write("".getBytes());
    // AnimatedGifEncoder animatedGifEncoder = new AnimatedGifEncoder();
    // int frameCount = gifDecoder.getFrameCount();
    // int loopCount = gifDecoder.getLoopCount();
    // animatedGifEncoder.setRepeat(loopCount);
    // animatedGifEncoder.start(outputStream);
    // for (int frameNumber = 0; frameNumber < frameCount; frameNumber++) {
    //
    // BufferedImage frame = gifDecoder.getFrame(frameNumber); // frame i
    // int delay = gifDecoder.getDelay(frameNumber); // display duration of frame in milliseconds
    // animatedGifEncoder.setDelay(delay); // frame delay per sec
    // BufferedImage scaleImage = scaleImage(frame, width, getWeb());
    // animatedGifEncoder.addFrame( scaleImage );
    // }
    // animatedGifEncoder.finish();
    // return outputStream.toByteArray();
    // }
    public static String getThumb() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.thumbnail");
    }

    public static String getWeb() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.web-resolution");
    }

    public static String getOrig() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("xsd.metadata.content-category.original-resolution");
    }
    /**
     * for reading CMYK images Creates new RGB images from all the CMYK images passed in on the command line.
     */
    // public static BufferedImage cmykRasterToSRGB(byte[] inputStream, String format)throws Exception{
    // //Find a suitable ImageReader
    // Iterator readers = ImageIO.getImageReadersByFormatName(format);
    // ImageReader reader = null;
    // while(readers.hasNext()) {
    // reader = (ImageReader)readers.next();
    // if(reader.canReadRaster()) {
    // break;
    // }
    // }
    // //Stream the image file (the original CMYK image)
    // ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(inputStream));
    // reader.setInput(input);
    // // Create the image.
    // BufferedImage image;
    // Raster raster = reader.readRaster(0, null);
    // // Arbitrarily select a BufferedImage type.
    // int imageType;
    // switch(raster.getNumBands())
    // {
    // case 1:
    // imageType = BufferedImage.TYPE_BYTE_GRAY;
    // break;
    // case 3:
    // imageType = BufferedImage.TYPE_3BYTE_BGR;
    // break;
    // case 4:
    // imageType = BufferedImage.TYPE_4BYTE_ABGR;
    // break;
    // default:
    // throw new UnsupportedOperationException();
    // }
    // // Create a BufferedImage.
    // image = new BufferedImage(raster.getWidth(),raster.getHeight(),imageType);
    // // Set the image data.
    // image.getRaster().setRect(raster);
    // return image;
    // }
    //
    // public static BufferedImage readCMYKwithJAI(byte[] inputStream, String format)throws Exception
    // {
    // ByteArrayInputStream bais = new ByteArrayInputStream(inputStream);
    // SeekableStream seekableStream = SeekableStream.wrapInputStream(bais,false);
    // PlanarImage src = JAI.create("Stream", seekableStream);
    // BufferedImage image = src.getAsBufferedImage();
    // return image;
    // }
    //
    // public static BufferedImage readCMYKwithjm4java(byte[] inputStream, String format)throws Exception
    // {
    // ByteArrayInputStream bais = new ByteArrayInputStream(inputStream);
    // Stream2BufferedImage stream4Image= new Stream2BufferedImage();
    // stream4Image.consumeOutput(bais);
    // BufferedImage image = stream4Image.getImage();
    // return image;
    // }
}
