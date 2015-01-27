package it.unitn.lode2.slide;

import javafx.scene.image.Image;

import java.net.URL;

/**
 * User: tiziano
 * Date: 01/01/15
 * Time: 15:19
 */
public interface Slide {

    public Image createPreview(Double width, Double height);

    public String getTitle();

    public String getDescription();

    public URL getUrl();

}
