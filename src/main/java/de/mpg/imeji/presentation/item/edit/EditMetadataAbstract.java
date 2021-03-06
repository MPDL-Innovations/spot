package de.mpg.imeji.presentation.item.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean to edit {@link Metadata} for a list of {@link Item}
 *
 * @author saquet
 *
 */
public abstract class EditMetadataAbstract extends SuperBean {
  private static final long serialVersionUID = -8870761990852602492L;
  private static final Logger LOGGER = LogManager.getLogger(EditMetadataAbstract.class);
  protected ItemService itemService = new ItemService();
  protected StatementService statementService = new StatementService();
  private List<SelectItem> statementMenu = new ArrayList<>();
  protected Map<String, Statement> statementMap = new HashMap<>();

  /**
   * Default constructor
   */
  public EditMetadataAbstract() {
    final StatementService statementService = new StatementService();
    try {
      statementMap = StatementUtil.statementListToMap(
          statementService.searchAndRetrieve(null, null, getSessionUser(), Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX));
      statementMenu = statementMap.keySet().stream().map(s -> new SelectItem(s)).collect(Collectors.toList());
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = "Error retrieving statements. " + exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
    } catch (final ImejiException e) {
      BeanHelper.error("Error retrieving statements");
      LOGGER.error("Error retrieving statements", e);
    }
  }

  /**
   * Save the items
   * 
   * @throws ImejiException
   */
  public void save() throws ImejiException {
    statementService.createBatchIfNotExists(getAllStatements(), getSessionUser());
    itemService.updateBatch(toItemList(), getSessionUser());
  }

  /**
   * Return the default Statement for the current items
   * 
   * @return
   */
  protected Map<String, Statement> getDefaultStatements() {
    final Map<String, Statement> map = new HashMap<>();
    try {
      // add from config
      map.putAll(StatementUtil.statementListToMap(retrieveInstanceDefaultStatements()));
    } catch (Exception e) {
      LOGGER.error("Error adding default statement to editor", e);
    }
    return map;
  }

  /**
   * Retrieve the default statements defined for the whole instance
   * 
   * @return
   * @throws ImejiException
   */
  private List<Statement> retrieveInstanceDefaultStatements() throws ImejiException {
    return statementService.retrieveBatch(StatementUtil.toStatementUriList(Imeji.CONFIG.getStatements()), Imeji.adminUser);
  }

  /**
   * Retrieve all the collections of the c
   * 
   * @return
   * @throws ImejiException
   */
  protected List<CollectionImeji> getItemsCollections() throws ImejiException {
    Set<String> colIds = new HashSet<>();
    for (Item item : toItemList()) {
      colIds.add(item.getCollection().toString());
    }
    return new CollectionService().retrieve(colIds.stream().collect(Collectors.toList()), getSessionUser());
  }

  /**
   * Convert the List of {@link ItemMetadataInputComponent} to a list of {@link Item}
   *
   * @return
   */
  public abstract List<Item> toItemList();

  /**
   * Return all Statement which are used by at least one metadata in at least one item
   * 
   * @return
   */
  public abstract List<Statement> getAllStatements();

  public List<SelectItem> getStatementMenu() {
    return statementMenu;
  }
}
