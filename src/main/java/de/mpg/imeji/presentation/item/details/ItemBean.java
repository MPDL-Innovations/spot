package de.mpg.imeji.presentation.item.details;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.TechnicalMetadata;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionObjectsController;

/**
 * Bean for a Single image
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ItemBean")
@ViewScoped
public class ItemBean extends SuperBean {
  private static final long serialVersionUID = -4957755233785015759L;
  private static final Logger LOGGER = Logger.getLogger(ItemBean.class);
  private Item item;
  private ContentVO content;
  private String id;
  private boolean selected;
  private CollectionImeji collection;
  private List<String> techMd;
  protected String prettyLink;
  private ItemDetailsBrowse browse = null;
  private String dateCreated;
  private String newFilename;
  private String imageUploader;
  private String discardComment;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  private int rotation;
  private int lastRotation = 0;
  private String thumbnail;
  private String preview;
  private String fullResolution;
  private String originalFile;
  private boolean edit = false;
  private ExecutorService rotationService;

  /**
   * Construct a default {@link ItemBean}
   *
   * @
   */
  public ItemBean() {
    prettyLink = "pretty:editImage";
  }

  /**
   * Initialize the {@link ItemBean}
   *
   * @return
   * @throws IOException @
   */
  @PostConstruct
  public void init() {
    this.id = UrlHelper.getParameterValue("id");
    rotationService = Executors.newSingleThreadExecutor();
    try {
      loadImage();
      if (item != null) {
        loadCollection(getSessionUser());
        initBrowsing();
        initImageUploader();
        selected = getSelectedItems().contains(item.getId().toString());
      }
    } catch (final NotFoundException e) {
      LOGGER.error("Error loading item", e);
      try {
        FacesContext.getCurrentInstance().getExternalContext().responseSendError(404,
            "404_NOT_FOUND");
      } catch (final IOException e1) {
        LOGGER.error("Error sending error", e1);;
      }
    } catch (final Exception e) {
      LOGGER.error("Error initialitzing item page", e);
      BeanHelper.error("Error initializing page" + e.getMessage());
    }
  }

  protected void initBrowsing() {
    browse = new ItemDetailsBrowse(item, "item", null, getSessionUser());
  }


  public String getOpenseadragonUrl() {
    return getNavigation().getOpenseadragonUrl() + "?id=" + content.getOriginal();
  }

  /**
   * Find the user name of the user who upload the file
   */
  private void initImageUploader() {
    final Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    final List<String> users =
        search.searchString(JenaCustomQueries.selectUserCompleteName(item.getCreatedBy()), null,
            Imeji.adminUser, 0, 1).getResults();
    if (users != null && users.size() > 0) {
      imageUploader = users.get(0);
    } else {
      imageUploader = Imeji.RESOURCE_BUNDLE.getLabel("unknown_user", getLocale());
    }
  }

  /**
   * Initialize the technical metadata when the "technical metadata" tab is called
   *
   * @throws ImejiException
   *
   * @
   */
  public void initViewTechnicalMetadata() throws ImejiException {
    techMd = new ArrayList<>();
    content = new ContentService().retrieve(content.getId().toString());
    for (final TechnicalMetadata tmd : content.getTechnicalMetadata()) {
      techMd.add(tmd.getName() + ": " + tmd.getValue());
    }
  }

  /**
   * Load the item according to the idntifier defined in the URL
   *
   * @throws ImejiException
   *
   * @
   */
  public void loadImage() throws ImejiException {
    item = new ItemService().retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
    if (item == null) {
      throw new NotFoundException("LoadImage: empty");
    }
    try {
      ContentService service = new ContentService();
      content = service.retrieve(service.findContentId(item.getId().toString()));
      this.preview = content.getPreview();
      this.thumbnail = content.getThumbnail();
      this.fullResolution = content.getFull();
      this.originalFile = content.getOriginal();
    } catch (final Exception e) {
      LOGGER.error("No content found for " + item.getIdString(), e);
    }
  }

  /**
   * Load the collection according to the identifier defined in the URL
   */
  public void loadCollection(User user) {
    try {
      collection = new CollectionService().retrieveLazy(item.getCollection(), user);
    } catch (final Exception e) {
      BeanHelper.error(e.getMessage());
      collection = null;
      LOGGER.error("Error loading collection", e);
    }
  }

  /**
   * Return and URL encoded version of the filename
   *
   * @return
   * @throws UnsupportedEncodingException
   */
  public String getEncodedFileName() throws UnsupportedEncodingException {
    if (item == null || item.getFilename() == null) {
      return "";
    }
    return URLEncoder.encode(item.getFilename(), "UTF-8");
  }

  public List<String> getTechMd() {
    return techMd;
  }

  public void setTechMd(List<String> md) {
    this.techMd = md;
  }

  public String getPageUrl() {
    return getHistory().getCurrentPage().getCompleteUrl();
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public void setImage(Item item) {
    this.item = item;
  }

  public Item getImage() {
    return item;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean getSelected() {
    return selected;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getNavigationString() {
    return "pretty:item";
  }

  public void saveEditor() throws IOException {
    try {
      BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_editor_image", getLocale()));
      redirect(getHistory().getCurrentPage().getCompleteUrl());
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_metadata_edit", getLocale()));
      LOGGER.error("Error saving item metadata", e);
    }
  }

  public void cancelEditor() throws Exception {
    redirect(getHistory().getCurrentPage().getCompleteUrl());
  }

  public void showEditor() {
    this.edit = true;
  }

  /**
   * Remove the {@link Item} from the database. If the item was in the current {@link Album}, remove
   * the {@link Item} from it
   *
   * @throws ImejiException
   *
   * @
   */
  public void delete() throws ImejiException {
    new ItemService().delete(Arrays.asList(item), getSessionUser());
    new SessionObjectsController().unselectItem(item.getId().toString());
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
        + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_collection_remove_from", getLocale()));
    redirectToBrowsePage();
  }

  /**
   * Discard the Item
   *
   * @throws ImejiException
   * @throws IOException
   */
  public void withdraw() throws ImejiException, IOException {
    new ItemService().withdraw(Arrays.asList(item), getDiscardComment(), getSessionUser());
    new SessionObjectsController().unselectItem(item.getId().toString());
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename()
        + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_item_withdraw", getLocale()));
    redirectToBrowsePage();
  }

  /**
   * Listener for the discard comment
   *
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    this.discardComment = event.getNewValue().toString();
  }

  /**
   * Redirect to the browse page
   *
   * @throws IOException
   */
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getBrowseUrl());
    } catch (final IOException e) {
      LOGGER.error("Error redirect to browse page", e);
    }
  }

  /**
   * Listener of the value of the select box
   *
   * @param event
   */
  public void selectedChanged(ValueChangeEvent event) {
    final SessionObjectsController soc = new SessionObjectsController();
    if (event.getNewValue().toString().equals("true")) {
      setSelected(true);
      soc.selectItem(item.getId().toString());
    } else if (event.getNewValue().toString().equals("false")) {
      setSelected(false);
      soc.unselectItem(item.getId().toString());
    }
  }

  public List<SelectItem> getStatementMenu() throws ImejiException {
    final List<SelectItem> statementMenu = new ArrayList<SelectItem>();
    for (final Statement s : new StatementService().searchAndRetrieve(null, null, getSessionUser(),
        -1, 0)) {
      statementMenu.add(new SelectItem(s.getIndex(), s.getIndex()));
    }
    return statementMenu;
  }

  public boolean isLocked() {
    return Locks.isLocked(this.item.getId().toString(), getSessionUser().getEmail());
  }

  public ItemDetailsBrowse getBrowse() {
    return browse;
  }

  public void setBrowse(ItemDetailsBrowse browse) {
    this.browse = browse;
  }

  public String getDescription() {
    return item.getFilename();
  }

  /**
   * Return the {@link User} having uploaded the file for this item
   *
   * @return @
   */
  public String getImageUploader() {
    return imageUploader;
  }


  /**
   * getter
   *
   * @return
   */
  public String getItemStorageIdFilename() {
    return StringHelper.normalizeFilename(this.item.getFilename());
  }

  /**
   * True if the current file is an image
   *
   * @return
   */
  public boolean isImageFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("image");
  }


  /**
   * True if the data can be viewed in the data viewer (defined in the configuration)
   *
   * @return
   */
  public boolean isViewInDataViewer() {
    return Imeji.CONFIG
        .isDataViewerSupportedFormats(FilenameUtils.getExtension(item.getFilename()));
  }


  /**
   * True if the file is an svg
   *
   * @return
   */
  public boolean isSVGFile() {
    return "svg".equals(FilenameUtils.getExtension(content.getOriginal()));
  }

  /**
   * True if the current file is a video
   *
   * @return
   */
  public boolean isVideoFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("video");
  }

  /**
   * True if the File is a RAW file (a file which can not be viewed in any online tool)
   *
   * @return
   */
  public boolean isRawFile() {
    return !isAudioFile() && !isVideoFile() && !isImageFile() && !isPdfFile();
  }

  /**
   * True if the current file is a pdf
   *
   * @return
   */
  public boolean isPdfFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("application/pdf");
  }

  /**
   * Function checks if the file ends with swc
   */
  public boolean isSwcFile() {
    return content.getOriginal().endsWith(".swc");
  }

  /**
   * True if the current file is an audio
   *
   * @return
   */
  public boolean isAudioFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename()))
        .contains("audio");
  }

  /**
   * @return the dateCreated
   */
  public String getDateCreated() {
    return dateCreated;
  }

  /**
   * @param dateCreated the dateCreated to set
   */
  public void setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getNewFilename() {
    this.newFilename = getImage().getFilename();
    return newFilename;
  }

  public void setNewFilename(String newFilename) {
    if (!"".equals(newFilename)) {
      getImage().setFilename(newFilename);
    }
  }

  /**
   * @return the discardComment
   */
  public String getDiscardComment() {
    return discardComment;
  }

  /**
   * @param discardComment the discardComment to set
   */
  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }


  public List<String> getSelectedItems() {
    return selectedItems;
  }

  public void setSelectedItems(List<String> selectedItems) {
    this.selectedItems = selectedItems;
  }

  /**
   * @return the content
   */
  public ContentVO getContent() {
    return content;
  }

  /**
   * @param content the content to set
   */
  public void setContent(ContentVO content) {
    this.content = content;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public int getThumbnailWidth() {
    return Integer.parseInt(Imeji.PROPERTIES.getProperty("xsd.resolution.thumbnail"));
  }


  /**
   * Called when a picture is rotated by Openseadragon. If the user is authorized to rotate the
   * image, webresolution and thumbnail get rotated
   *
   * @throws Exception
   * @throws IOException
   */
  public void updateRotation() throws IOException, Exception {
    if (SecurityUtil.authorization().update(getSessionUser(), getImage())) {
      int degrees = (rotation - lastRotation + 360) % 360;
      lastRotation = rotation;
      rotationService.submit(new RotationJob(degrees));
    }
  }

  private class RotationJob implements Callable<Integer> {
    int degrees;

    public RotationJob(int degrees) {
      this.degrees = degrees;
    }

    @Override
    public Integer call() throws Exception {
      StorageController storageController = new StorageController();
      storageController.rotate(getContent().getFull(), degrees);

      long width = getContent().getWidth();
      getContent().setWidth(getContent().getHeight());
      getContent().setHeight(width);
      setContent(new ContentService().update(getContent()));
      return 1;
    }
  }

  /**
   * Gets the width of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionWidth() throws IOException {
    int webSize = Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
    int imgWidth = (int) getContent().getWidth();
    int imgHeight = (int) getContent().getHeight();
    if (imgWidth < webSize && imgHeight < webSize) {
      return imgWidth;
    }
    if (imgWidth >= imgHeight) {
      return webSize;
    }
    return (int) (imgWidth * 1.0 / imgHeight * webSize);
  }

  /**
   * Gets the height of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionHeight() throws IOException {
    int webSize = Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
    int imgWidth = (int) getContent().getWidth();
    int imgHeight = (int) getContent().getHeight();
    if (imgWidth < webSize && imgHeight < webSize) {
      return imgHeight;
    }
    if (imgWidth >= imgHeight) {
      return (int) (imgHeight * 1.0 / imgWidth * webSize);
    }
    return webSize;
  }

  /**
   * Gets the max of width and height of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionMaxLength() throws IOException {
    return Math.max(getWebResolutionWidth(), getWebResolutionHeight());
  }

  public int getFullResolutionWidth() throws IOException {
    return (int) getContent().getWidth();
  }

  public int getFullResolutionHeight() throws IOException {
    return (int) getContent().getHeight();
  }

  public int getWebResolutionTop() throws IOException {
    return (getWebResolutionMaxLength() - getWebResolutionHeight()) / 2;
  }

  public int getWebResolutionLeft() throws IOException {
    return (getWebResolutionMaxLength() - getWebResolutionWidth()) / 2;
  }

  public int getRotation() {
    return rotation;
  }

  public void setRotation(int rotation) {
    this.rotation = rotation;
  }

  public boolean isViewInOpenseadragon() {
    return !isViewInDataViewer() && isImageFile() && !isSVGFile() && !isGIFFile();
  }

  /**
   * True if the file ia a gif
   *
   * @return
   */
  public boolean isGIFFile() {
    return "gif".equals(FilenameUtils.getExtension(content.getOriginal()));
  }

  /**
   * @return the thumbnail
   */
  public String getThumbnail() {
    return thumbnail;
  }

  /**
   * @return the preview
   */
  public String getPreview() {
    return preview;
  }

  /**
   * @return the fullResolution
   */
  public String getFullResolution() {
    return fullResolution;
  }

  /**
   * @return the originalFile
   */
  public String getOriginalFile() {
    return originalFile;
  }

  /**
   * @return the edit
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * @param edit the edit to set
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }


}