/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * imeji collection has one {@link MetadataProfile} and contains {@link Item}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/collection")
@j2jModel("collection")
@j2jId(getMethod = "getId", setMethod = "setId")
public class CollectionImeji extends Container implements Serializable {
  private static final long serialVersionUID = -4689209760815149573L;

  private Collection<URI> images = new ArrayList<URI>();

  @Override
  public void setImages(Collection<URI> images) {
    this.images = images;
  }

  @Override
  public Collection<URI> getImages() {
    return images;
  }

}
