package de.mpg.imeji.logic.search;

import java.util.List;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;

/**
 * Search Interface for imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Search {

  /**
   * Use this constant to state that you want to retrieve all existing search results of a query
   * without any size limits
   */
  public static final int GET_ALL_RESULTS = -1000; //

  /**
   * Use this constant to state that you want to retrieve data starting at result index 0
   */
  public static final int SEARCH_FROM_START_INDEX = 0;

  /**
   * Types of search (What objects types are retuned)
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public static enum SearchObjectTypes {
    ITEM,
    COLLECTION,
    USER,
    USERGROUPS,
    ALL,
    CONTENT,
    STATEMENT;

    private SearchObjectTypes() {
      // TODO Auto-generated constructor stub
    }

    public static SearchObjectTypes getFromDataType(Class<?> dataObjectClass) {
      if (dataObjectClass.equals(Item.class)) {
        return ITEM;
      } else if (dataObjectClass.equals(CollectionImeji.class)) {
        return COLLECTION;
      } else if (dataObjectClass.equals(User.class)) {
        return USER;
      } else if (dataObjectClass.equals(UserGroup.class)) {
        return USERGROUPS;
      } else if (dataObjectClass.equals(ContentVO.class)) {
        return CONTENT;
      } else if (dataObjectClass.equals(Statement.class)) {
        return STATEMENT;
      } else {
        // LOG Error
        return null;
      }

    }
  }

  /**
   * Search for imeji objects
   *
   * @param query
   * @param sortCri
   * @param user
   * @param folderUri TODO
   * @param from
   * @param size
   * @return
   */
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri, int offset, int size);

  /**
   * Search for Imeji objects Employ multilevel sorting on results (i.e. sort all results with a
   * first sort criterion, then sort all objects that fall into the same category with a second sort
   * criterion)
   * 
   * @param query
   * @param sortCriteria
   * @param user
   * @param folderUri
   * @param offset
   * @param size
   * @return
   */
  public SearchResult searchWithMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int offset, int size);

  /**
   * Search and set {@link Facet} to the {@link SearchResult}
   * 
   * @param query
   * @param sortCri
   * @param user
   * @param folderUri
   * @param offset
   * @param size
   * @return
   */
  public SearchResult searchWithFacets(SearchQuery query, SortCriterion sortCri, User user, String folderUri, int offset, int size);

  /**
   * Search Imeji objects - with {@link Facet}s and - apply multi-level sorting to results
   *
   * @param query
   * @param sortCriteria
   * @param user
   * @param folderUri
   * @param from
   * @param size
   * @return
   */
  public SearchResult searchWithFacetsAndMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size, boolean includeSubcollections);

  /**
   * Get the {@link SearchIndexer} for this {@link Search} implementation
   *
   * @return
   */
  public SearchIndexer getIndexer();

  /**
   * Search for imeji objects belonging to a predefined list of possible results
   *
   * @param query
   * @param sortCri
   * @param user
   * @param uris
   * @return
   */
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, List<String> uris);

  /**
   * Search with a Simple {@link String}
   *
   * @param query
   * @param sort
   * @param user
   * @param from
   * @param size
   * @return
   */
  public SearchResult searchString(String query, SortCriterion sort, User user, int from, int size);

}
