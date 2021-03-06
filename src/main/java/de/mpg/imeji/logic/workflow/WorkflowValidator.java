package de.mpg.imeji.logic.workflow;

import java.io.Serializable;

import de.mpg.imeji.exceptions.NotSupportedMethodException;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;

/**
 * Check update and delete Operation against the imeji Workflow. If not compliant, throw an error
 *
 * @author bastiens
 *
 */
public class WorkflowValidator implements Serializable {
  private static final long serialVersionUID = -3583312940203422191L;

  /**
   * Object can be deleted if:<br/>
   * * Status is PENDING
   *
   * @param p
   * @throws WorkflowException
   */
  public void isDeleteAllowed(Properties p) throws WorkflowException {
    if (p.getStatus() != Status.PENDING) {
      throw new WorkflowException(
          "Workflow operation not allowed: " + p.getId() + " can not be deleted (current status: " + p.getStatus() + ")",
          "collection_or_item_is_not_pending");
    }
  }

  /**
   * Object can be created if: <br/>
   * * if private mode, object must have Status PENDING
   *
   * @param p
   * @throws WorkflowException
   */
  public void isCreateAllowed(Properties p) throws WorkflowException {
    if (p.getStatus() == Status.WITHDRAWN) {
      throw new WorkflowException("Can not create discarded object", null);
    }
    if (Imeji.CONFIG.getPrivateModus() && p.getStatus() != Status.PENDING) {
      throw new WorkflowException("Object publication is disabled!", "publication_disabled");
    }
  }

  /**
   * Object can edited if:
   * <li>object is not discarded
   * 
   * @param p
   * @throws WorkflowException
   */
  public void isUpdateAllowed(Properties p) throws WorkflowException {
    if (p.getStatus() == Status.WITHDRAWN) {
      String userErrorMessageLabel = null;
      if (p instanceof CollectionImeji) {
        userErrorMessageLabel = "error_update_withdrawn_collection";
      } else if (p instanceof Item) {
        userErrorMessageLabel = "error_update_withdrawn_item";
      }
      throw new WorkflowException("Can not update discarded object", userErrorMessageLabel);
    }
  }

  /**
   * DOI can be created if: <br/>
   * * imeji is not in private mode <br/>
   * * Status is PENDING or RELEASED
   *
   * @param p
   * @throws WorkflowException
   */

  public void isCreateDOIAllowed(Properties p) throws WorkflowException {
    if (Imeji.CONFIG.getPrivateModus() && !isSubcollection(p)) {
      throw new WorkflowException("DOI is not allowed in private mode", null);
    }
    if (p.getStatus() != Status.RELEASED && p.getStatus() != Status.PENDING) {
      throw new WorkflowException("DOI is only allowed for pending and released items", null);
    }

  }

  /**
   * Can be release if: <br/>
   * * imeji is not in private Modus <br/>
   * * Status is PENDING <br/>
   *
   * @param p
   * @return
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   */
  public void isReleaseAllowed(Properties p) throws WorkflowException, NotSupportedMethodException {
    if (Imeji.CONFIG.getPrivateModus() && !isSubcollection(p)) {
      throw new NotSupportedMethodException("Object publication is disabled!");
    }

    if (p.getStatus() == Status.RELEASED) {
      throw new WorkflowException("Only PENDING objects can be released", "error_release_released_collection");
    } else if (p.getStatus() == Status.WITHDRAWN) {
      throw new WorkflowException("Only PENDING objects can be released", "error_release_withdrawn_collection");
    }
  }

  /**
   * Object can be withdrawn if:<br/>
   * * Status is RELEASED
   *
   * @param p
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   */
  public void isWithdrawAllowed(Properties p) throws WorkflowException, NotSupportedMethodException {
    if (p.getStatus() == Status.PENDING) {
      throw new WorkflowException("Only RELEASED objects can be withdrawn", null);
    } else if (p.getStatus() == Status.WITHDRAWN) {
      throw new WorkflowException("Only RELEASED objects can be withdrawn", "error_withdraw_withdrawn_object");
    }
  }

  private boolean isSubcollection(Properties p) {
    return p instanceof CollectionImeji && ((CollectionImeji) p).isSubCollection();
  }
}
