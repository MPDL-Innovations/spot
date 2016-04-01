/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.mdProfile.ProfileSelector;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.VocabularyHelper;

/**
 * Java Bean for the create Collection Page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CreateCollectionBean")
@ViewScoped
public class CreateCollectionBean extends CollectionBean {
  private static final Logger LOGGER = Logger.getLogger(CreateCollectionBean.class);
  private static final long serialVersionUID = 1257698224590957642L;
  private final VocabularyHelper vocabularyHelper = new VocabularyHelper();;
  private ProfileSelector profileSelector;
  private boolean createProfile = false;

  /**
   * Bean Constructor
   */
  public CreateCollectionBean() {
    initialize();
  }

  /**
   * Method called when paged is loaded (defined in pretty-config.xml)
   */
  public void initialize() {
    setCollectionCreateMode(true);
    setCollection(ImejiFactory.newCollection());
    ((List<Person>) getCollection().getMetadata().getPersons()).set(0,
        sessionBean.getUser().getPerson().clone());
    profileSelector =
        new ProfileSelector(null, sessionBean.getUser(), sessionBean.getSelectedSpaceString());
  }

  /**
   * Method for save button. Create the {@link CollectionImeji} according to the form
   * 
   * @return
   * @throws Exception
   */
  public String save() {
    if (createCollection()) {
      try {
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(navigation.getCollectionUrl() + getCollection().getIdString());
      } catch (IOException e) {
        LOGGER.error("Error redirecting after saving collection", e);
      }
    }
    return "";
  }

  /**
   * Method for save&editProfile button. Create the {@link CollectionImeji} according to the form
   * 
   * @return
   * @throws Exception
   */
  public String saveAndEditProfile() throws Exception {
    if (createCollection()) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(navigation.getProfileUrl() + extractIDFromURI(getCollection().getProfile())
              + "/edit?init=1&col=" + getCollection().getIdString());
    }
    return "";
  }

  /**
   * Create the collection and its profile
   * 
   * @return
   * @throws Exception
   */
  public boolean createCollection() {
    try {
      CollectionController collectionController = new CollectionController();
      int pos = 0;
      // Set the position of the persons and organizations (used for the sorting later)
      for (Person p : getCollection().getMetadata().getPersons()) {
        p.setPos(pos);
        pos++;
        int pos2 = 0;
        for (Organization o : p.getOrganizations()) {
          o.setPos(pos2);
          pos2++;
        }
      }
      if (!createProfile) {
        profileSelector.setProfile(null);
      }
      URI id = collectionController.create(getCollection(), profileSelector.getProfile(),
          sessionBean.getUser(), profileSelector.getSelectorMode(),
          sessionBean.getSelectedSpaceString());
      setCollection(collectionController.retrieve(id, sessionBean.getUser()));
      setId(ObjectHelper.getId(id));
      setSendEmailNotification(isSendEmailNotification());
      UserController uc = new UserController(sessionBean.getUser());
      uc.update(sessionBean.getUser(), sessionBean.getUser());
      BeanHelper.info(
          Imeji.RESOURCE_BUNDLE.getMessage("success_collection_create", sessionBean.getLocale()));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.cleanMessages();
      for (String m : e.getMessages()) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(m, sessionBean.getLocale()));
      }
    } catch (ImejiException e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage(e.getLocalizedMessage(), sessionBean.getLocale()));
    }
    return false;
  }

  public static String extractIDFromURI(URI uri) {
    return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
  }

  /**
   * Return the link for the Cancel button
   * 
   * @return
   */
  public String getCancel() {
    return navigation.getCollectionsUrl() + "?q=";
  }

  @Override
  protected String getNavigationString() {
    return sessionBean.getPrettySpacePage("pretty:createCollection");
  }

  public String getVocabularyLabel(URI id) {
    for (SelectItem item : vocabularyHelper.getVocabularies()) {

      if (id.toString().equals(item.getValue().toString())) {
        return item.getLabel();
      }
    }
    return "";
  }

  public ProfileSelector getProfileSelector() {
    return profileSelector;
  }

  public void setProfileSelector(ProfileSelector profileSelector) {
    this.profileSelector = profileSelector;
  }

  public boolean isCreateProfile() {
    return createProfile;
  }

  public void setCreateProfile(boolean createProfile) {
    this.createProfile = createProfile;
  }

}
