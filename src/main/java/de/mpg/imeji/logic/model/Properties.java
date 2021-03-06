package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Calendar;

import javax.xml.bind.annotation.XmlEnum;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.model.aspects.AccessMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;

/**
 * Common properties to all imeji objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/properties")
public class Properties implements Serializable, ResourceLastModified, AccessMember {
  private static final long serialVersionUID = 6874979775433576816L;
  private URI id;
  @j2jResource(ImejiNamespaces.CREATOR)
  private URI createdBy = URI.create("");
  @j2jResource(ImejiNamespaces.MODIFIED_BY)
  private URI modifiedBy = URI.create("");;
  @j2jLiteral(ImejiNamespaces.DATE_CREATED)
  private Calendar created = Calendar.getInstance();
  @j2jLiteral(ImejiNamespaces.VERSION_DATE)
  private Calendar versionDate;
  @j2jLiteral(ImejiNamespaces.LAST_MODIFICATION_DATE)
  private Calendar modified = Calendar.getInstance();
  @j2jResource(ImejiNamespaces.STATUS)
  private URI status = URI.create(Status.PENDING.getUriString());
  @j2jLiteral(ImejiNamespaces.DISCARD_COMMENT)
  private String discardComment;

  private static final Logger LOGGER = LogManager.getLogger(Properties.class);

  @XmlEnum(String.class)
  public enum Status {
    PENDING(new String(ImejiNamespaces.STATUS + "#PENDING")),
    RELEASED(new String(ImejiNamespaces.STATUS + "#RELEASED")),
    WITHDRAWN(new String(ImejiNamespaces.STATUS + "#WITHDRAWN"));
    private String uri;

    private Status(String uri) {
      this.uri = uri;
    }

    public String getUriString() {
      return uri;
    }

    public URI getURI() {
      return URI.create(uri);
    }
  }

  public Properties() {

  }

  @Override
  public void accessMember(ChangeMember changeMember) {

    // members that can be edited unsynchronized:
    // (a) versionDate (ADD, EDIT) [in release, withdraw]
    // (b) status (EDIT) [in release, withdraw]
    // (c) discard comment (ADD) [in withdraw]

    Field fieldToSet = changeMember.getField();

    try {
      Field versionDateField = Properties.class.getDeclaredField("versionDate");
      Field statusField = Properties.class.getDeclaredField("status");
      Field discardCommentField = Properties.class.getDeclaredField("discardComment");

      // (a) versionDate
      if (fieldToSet.equals(versionDateField) && changeMember.getValue() instanceof Calendar
          && changeMember.getAction().equals(ActionType.ADD) && this.versionDate == null) {
        this.versionDate = (Calendar) changeMember.getValue();
      } else if (fieldToSet.equals(versionDateField) && changeMember.getValue() instanceof Calendar
          && changeMember.getAction().equals(ActionType.EDIT)) {
        this.versionDate = (Calendar) changeMember.getValue();
      }
      // (b) status field
      else if (fieldToSet.equals(statusField) && changeMember.getValue() instanceof Status
          && changeMember.getAction().equals(ActionType.EDIT)) {
        this.status = ((Status) changeMember.getValue()).getURI();
      }
      // (c) comment field
      else if (fieldToSet.equals(discardCommentField) && changeMember.getValue() instanceof String
          && changeMember.getAction().equals(ActionType.ADD) && this.discardComment.isEmpty()) {
        this.discardComment = (String) changeMember.getValue();
      } else {
        LOGGER.debug("Did not edit member in Properties.");
      }
    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error("Could not edit member in Properties. Member does not exist in class.", e);
    }
  }


  public void setCreatedBy(URI createdBy) {
    this.createdBy = createdBy;
  }

  public URI getCreatedBy() {
    return createdBy;
  }

  public void setModifiedBy(URI modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public URI getModifiedBy() {
    return modifiedBy;
  }

  public void setStatus(Status status) {
    this.status = URI.create(status.getUriString());
  }

  public Status getStatus() {
    return Status.valueOf(status.getFragment());
  }

  public String getDiscardComment() {
    return discardComment;
  }

  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  public Calendar getCreated() {
    return created;
  }

  public void setCreated(Calendar created) {
    this.created = created;
  }

  public Calendar getModified() {
    return modified;
  }

  public void setModified(Calendar modified) {
    this.modified = modified;
  }

  public Field getTimeStampField() {
    try {
      return Properties.class.getDeclaredField("modified");
    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  /**
   * @return the versionDate
   */
  public Calendar getVersionDate() {
    return versionDate;
  }

  /**
   * @param versionDate the versionDate to set
   */
  public void setVersionDate(Calendar versionDate) {
    this.versionDate = versionDate;
  }


  /**
   * return the id of this object defined in the last number in its {@link URI}.
   *
   * @return
   */
  public String getIdString() {
    if (id != null) {
      return id.getPath().substring(id.getPath().lastIndexOf("/") + 1);
    }
    return "";
  }

  /**
   * TODO : check this method
   *
   * @param methodName
   * @return
   */
  public Object getValueFromMethod(String methodName) {
    Method method;
    Object ret = null;
    try {
      method = this.getClass().getMethod(methodName);
      ret = method.invoke(this);
    } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
      LOGGER.error("Some issues with getValueFromMethod ", e);
    }
    return ret;
  }



}
