package boofcv.examples.segmentation;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by sergey on 25.05.16.
 */
public class ResizeImage {
    public BufferedImage resizing(BufferedImage image){

        int IMG_WIDTH;
        int IMG_HEIGHT;

        IMG_WIDTH = image.getHeight();
        IMG_HEIGHT = image.getHeight();

        int type = image.getType() == 0? BufferedImage.TYPE_INT_ARGB : image.getType();


        BufferedImage resizeImageHintJpg = resizeImageWithHint(image, type, IMG_HEIGHT, IMG_WIDTH);
        return resizeImageHintJpg;
    }


    private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int type, int IMG_HEIGHT, int IMG_WIDTH){

        BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        return resizedImage;
    }
}
