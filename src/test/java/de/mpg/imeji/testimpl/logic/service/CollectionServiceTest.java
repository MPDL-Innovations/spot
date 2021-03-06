package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.concurrency.Lock;
import de.mpg.imeji.logic.concurrency.Locks;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ImejiTestResources;

/**
 * Tests CollectionService. Currently not tested: search, reindex
 * 
 * @author jandura
 *
 */
public class CollectionServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(CollectionServiceTest.class);

  private static User defaultUser;
  private static User userEditGrant;
  private static User userReadGrant;
  private static User userNoGrant;
  private static User restrictedUser;
  private static User sysadmin;

  private static CollectionImeji collectionPrivate;
  private static CollectionImeji collectionReleased;

  private static UserService userService;

  //TODO: Create separate test classes to test the CollectionService CRUD methods and link them here
  //Like: CollectionServiceCreateTest

  @BeforeClass
  public static void specificSetup() {
    userService = new UserService();
    try {
      sysadmin = ImejiFactory.newUser().setEmail("admin3@test.org").setPerson("admin3", "admin3", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      defaultUser = ImejiFactory.newUser().setEmail("default@test.org").setPerson("default", "default", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      restrictedUser = ImejiFactory.newUser().setEmail("restricted@test.org").setPerson("restricted", "restricted", "org")
          .setPassword("password").setQuota(Long.MAX_VALUE).build();
      userReadGrant = ImejiFactory.newUser().setEmail("read2@test.org").setPerson("read", "read", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userEditGrant = ImejiFactory.newUser().setEmail("edit2@test.org").setPerson("edit", "edit", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userNoGrant = ImejiFactory.newUser().setEmail("no2@test.org").setPerson("no", "no", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();

      sysadmin = userService.create(sysadmin, USER_TYPE.ADMIN);
      defaultUser = userService.create(defaultUser, USER_TYPE.DEFAULT);
      userEditGrant = userService.create(userEditGrant, USER_TYPE.DEFAULT);
      userReadGrant = userService.create(userReadGrant, USER_TYPE.DEFAULT);
      userNoGrant = userService.create(userNoGrant, USER_TYPE.DEFAULT);
      restrictedUser = userService.create(restrictedUser, USER_TYPE.RESTRICTED);

      CollectionService collectionService = new CollectionService();
      collectionPrivate = ImejiFactory.newCollection().setTitle("Private Collection").setPerson("Max", "Planck", "MPDL").build();
      collectionReleased = ImejiFactory.newCollection().setTitle("Released Collection").setPerson("Max", "Planck", "MPDL").build();

      collectionPrivate = collectionService.create(collectionPrivate, defaultUser);
      collectionReleased = collectionService.create(collectionReleased, defaultUser);
      defaultUser = userService.retrieve(defaultUser.getId(), sysadmin);
      Item releasedItem = ImejiFactory.newItem(collectionReleased);
      (new ItemService()).create(releasedItem, collectionReleased, defaultUser);
      collectionService.releaseWithDefaultLicense(collectionReleased, defaultUser);
      collectionReleased = collectionService.retrieve(collectionReleased.getId(), sysadmin);

      userReadGrant.getGrants().add(new Grant(GrantType.READ, collectionPrivate.getId().toString()).toGrantString());
      userEditGrant.getGrants().add(new Grant(GrantType.EDIT, collectionPrivate.getId().toString()).toGrantString());

      userService.update(userReadGrant, sysadmin);
      userService.update(userEditGrant, sysadmin);

    } catch (ImejiException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void create() {
    CollectionImeji testCreate = ImejiFactory.newCollection().setTitle("create").setPerson("p", "p", "p").build();
    create_Test("sysadmin", testCreate, sysadmin, null);
    create_Test("default user", testCreate, defaultUser, null);
    create_Test("restricted user", testCreate, restrictedUser, NotAllowedError.class);
  }

  private void create_Test(String msg, CollectionImeji col, User user, Class exception) {
    CollectionService service = new CollectionService();
    try {
      try {
        service.create(col, user);
        user = userService.retrieve(user.getId(), sysadmin);
      } catch (ImejiException e) {
        if (!e.getClass().equals(exception)) {
          Assert.fail(msg + ": " + e.getMessage());
        }
        return;
      }
      if (exception != null) {
        Assert.fail(msg + ": No exception has been thrown");
      }
      try {
        service.retrieve(Arrays.asList(col.getId().toString()), sysadmin); // Check if collection
                                                                           // has been created
        Assert.assertEquals(msg + ": User should have admin grant for collection", true,
            hasGrant(user.getGrants(), new Grant(GrantType.ADMIN, col.getId().toString())));
      } catch (ImejiException e) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    } finally {
      try {
        service.delete(col, sysadmin);
      } catch (ImejiException e) {
      }
    }
  }

  @Test
  public void retrieveRetrieveLazyAndRetrieveBatch() {
    retrieve_Test("No grant user, private collection", collectionPrivate.getId(), userNoGrant, collectionPrivate.getIdString(),
        collectionPrivate.getTitle(), NotAllowedError.class);
    retrieve_Test("Read grant user, private collection", collectionPrivate.getId(), userReadGrant, collectionPrivate.getIdString(),
        collectionPrivate.getTitle(), null);
    retrieve_Test("No grant user, released collection", collectionReleased.getId(), userNoGrant, collectionReleased.getIdString(),
        collectionReleased.getTitle(), null);
  }

  private void retrieve_Test(String msg, URI uri, User user, String expectedIDString, String expectedName, Class exception) {
    CollectionService service = new CollectionService();
    CollectionImeji res = null, res2 = null, res3 = null;
    try {
      res = service.retrieve(uri, user);
      res2 = service.retrieveLazy(uri, user);
      res3 = service.retrieve(Arrays.asList(uri.toString()), user).get(0);
    } catch (ImejiException e) {
      if (e.getClass().equals(exception)) {
        return;
      }
      Assert.fail(msg + ": " + e.getMessage());
    }
    if (exception != null) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    Assert.assertEquals(msg + ": ID should be equal", expectedIDString, res.getIdString());
    Assert.assertEquals(msg + ": Name should be equal", expectedName, res.getTitle());
    Assert.assertEquals(msg + " lazy: ID should be equal", expectedIDString, res2.getIdString());
    Assert.assertEquals(msg + " lazy: Name should be equal", expectedName, res2.getTitle());
    Assert.assertEquals(msg + " batch: ID should be equal", expectedIDString, res3.getIdString());
    Assert.assertEquals(msg + " batch: Name should be equal", expectedName, res3.getTitle());
  }

  @Test
  public void update() {
    collectionPrivate.setTitle("TestTitle");
    update_Test("private collection, user read grant", collectionPrivate, userReadGrant, "Private Collection", NotAllowedError.class);
    update_Test("private collection, user edit grant", collectionPrivate, userEditGrant, "Private Collection", null);
  }

  private void update_Test(String msg, CollectionImeji col, User user, String oldCollectionTitle, Class exception) {
    CollectionService service = new CollectionService();
    try {
      service.update(col, user);
    } catch (ImejiException e) {
      if (e.getClass().equals(exception)) {
        CollectionImeji res = null;
        try {
          res = service.retrieve(col.getId(), sysadmin);
        } catch (ImejiException e1) {
          Assert.fail(msg + ": " + e.getMessage());
        }
        Assert.assertEquals(msg + ":Collection title should not have changed", oldCollectionTitle, res.getTitle());
        return;
      }
      Assert.fail(msg + ": " + e.getMessage());
    }
    if (exception != null) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    try {
      CollectionImeji res = service.retrieve(col.getId(), user);
      Assert.assertEquals(msg + ": Collection title should be updated", col.getTitle(), res.getTitle());
    } catch (ImejiException e) {
      Assert.fail(msg + ": " + e.getMessage());
    }
  }

  @Test
  @Ignore
  public void updateLogo() {
    try {
      updateLogo_Test("collectionPrivate, edit grant user", collectionPrivate, ImejiTestResources.getTest1Jpg(), userEditGrant, null);

      updateLogo_Test("collectionPrivate, read grant user", collectionPrivate, ImejiTestResources.getTest2Jpg(), userReadGrant,
          NotAllowedError.class);

      // Check extra for removing logo
      try {
        (new CollectionService()).updateLogo(collectionPrivate, null, userEditGrant);
        if (collectionPrivate.getLogoUrl() != null) {
          Assert.fail("Remove collection: URL should be null");
        }
      } catch (IOException | URISyntaxException e) {
        Assert.fail(e.getMessage());
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void updateLogo_Test(String msg, CollectionImeji col, File file, User user, Class exception) {
    CollectionService service = new CollectionService();
    boolean exc = false;
    String previousChecksum = "";
    try {
      previousChecksum = col.getLogoUrl() == null ? "" : getLogoChecksum(col);
      service.updateLogo(col, file, user);
    } catch (Exception e) {
      exc = true;
      if (!e.getClass().equals(exception)) {
        e.printStackTrace();
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    if (exception != null && !exc) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    try {
      // Just check if the checksum changed, don't compare it with the checksum of the
      // uploaded
      // file, because we want to allow modification, e.g resizing
      CollectionImeji ret = service.retrieve(col.getId(), sysadmin);
      Assert.assertEquals(msg + ": If there should be no exception, the checksum should have changed",
          getLogoChecksum(ret).equals(previousChecksum), exception != null);
    } catch (IOException | ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void delete() {
    CollectionService collectionService = new CollectionService();
    ItemService itemService = new ItemService();
    CollectionImeji collectionToDelete = ImejiFactory.newCollection().setTitle("Delete").setPerson("p", "p", "o").build();
    try {
      collectionService.create(collectionToDelete, defaultUser);
      defaultUser = userService.retrieve(defaultUser.getId(), sysadmin);
      Item itemToDelete = ImejiFactory.newItem(collectionToDelete);
      itemService.createWithFile(itemToDelete, ImejiTestResources.getTestJpg(), "Test.jpg", collectionToDelete, defaultUser);
      delete_Test("private Collection, edit grant user", collectionToDelete, userEditGrant, NotAllowedError.class);
      Lock lock = new Lock(itemToDelete.getId().toString(), null);
      Locks.lock(lock);
      delete_Test("private Collection, collection admin user,itemLocked", collectionToDelete, defaultUser, UnprocessableError.class);
      Locks.unLock(lock);
      delete_Test("private Collection, collection admin user", collectionToDelete, defaultUser, null);
      delete_Test("released Collection, collection admin use", collectionReleased, defaultUser, WorkflowException.class);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void delete_Test(String msg, CollectionImeji col, User user, Class exception) {
    CollectionService service = new CollectionService();
    ItemService itemService = new ItemService();
    URI collectionID = col.getId();
    Collection<URI> itemIDs = col.getImages();
    boolean exc = false;
    try {
      service.delete(col, user);
    } catch (ImejiException e) {
      exc = true;
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    if (exception != null && !exc) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    if (exception == null) {
      try {
        service.retrieve(collectionID, sysadmin);
        Assert.fail(msg + ": It was possible to retrive collection after delete");
      } catch (ImejiException e) {
        if (!(e instanceof NotFoundException)) {
          Assert.fail(e.getMessage());
        }
      }
      for (URI id : itemIDs) {
        try {
          itemService.retrieve(id, sysadmin);
          Assert.fail(msg + ": It was possible to retrive item after delete");
        } catch (ImejiException e) {
          if (!(e instanceof NotFoundException)) {
            Assert.fail(e.getMessage());
          }
        }
      }
    } else {
      try {
        service.retrieve(collectionID, sysadmin);
        for (URI id : itemIDs) {
          itemService.retrieve(id, sysadmin);
        }
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void release() {
    CollectionService collectionService = new CollectionService();
    CollectionImeji collectionToRelease = ImejiFactory.newCollection().setTitle("To Release").setPerson("m", "p", "g").build();
    try {
      collectionService.create(collectionToRelease, defaultUser);
      defaultUser = userService.retrieve(defaultUser.getId(), sysadmin);

      release_Test("Empty collection", collectionToRelease, null, defaultUser, getDefaultLicense(), getDefaultLicense(),
          UnprocessableError.class);
      collectionToRelease = collectionService.retrieve(collectionToRelease.getId(), sysadmin);

      Item itemToRelease = ImejiFactory.newItem(collectionToRelease);
      itemToRelease.getLicenses().add(getDefaultLicense());
      (new ItemService()).create(itemToRelease, collectionToRelease, defaultUser);
      userEditGrant.getGrants().add(new Grant(GrantType.READ, collectionToRelease.getId().toString()).toGrantString());
      (new UserService()).update(userEditGrant, sysadmin);

      release_Test("Unauthorized user", collectionToRelease, itemToRelease, userEditGrant, getDefaultLicense(), getDefaultLicense(),
          NotAllowedError.class);
      collectionToRelease = collectionService.retrieve(collectionToRelease.getId(), sysadmin);

      Lock lock = new Lock(itemToRelease.getId().toString(), null);
      Locks.lock(lock);
      release_Test("Locked item", collectionToRelease, itemToRelease, defaultUser, getDefaultLicense(), getDefaultLicense(),
          UnprocessableError.class);
      Locks.unLock(lock);
      collectionToRelease = collectionService.retrieve(collectionToRelease.getId(), sysadmin);

      release_Test("pending collection, collection admin", collectionToRelease, itemToRelease, defaultUser, getDefaultLicense(),
          getDefaultLicense(), null);
      collectionToRelease = collectionService.retrieve(collectionToRelease.getId(), sysadmin);
      itemToRelease = (new ItemService()).retrieve(itemToRelease.getId().toString(), sysadmin);

      release_Test("Already released collection", collectionToRelease, itemToRelease, defaultUser, getDefaultLicense(), getDefaultLicense(),
          WorkflowException.class);

    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

  private void release_Test(String msg, CollectionImeji col, Item item, User user, License defaultLicense, License expectedLicense,
      Class exception) {
    CollectionService service = new CollectionService();
    Status previousColStatus = col.getStatus();
    Status previousItemStatus = item == null ? null : item.getStatus();
    try {
      service.release(col, user, defaultLicense);
      if (exception != null) {
        Assert.fail(msg + ": No exception has been thrown");
      }
    } catch (ImejiException e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    try {
      CollectionImeji retrivedCol = service.retrieve(col.getId(), sysadmin);
      Item retrievedItem = item == null ? null : (new ItemService()).retrieve(item.getId(), sysadmin);

      if (exception == null) {
        Assert.assertEquals(msg + ": Status of collection should be released", Status.RELEASED, retrivedCol.getStatus());
        if (item != null) {
          Assert.assertEquals(msg + "item status should be released", Status.RELEASED, retrievedItem.getStatus());
          Assert.assertEquals(msg + "item license name should be as expected", expectedLicense.getName(),
              retrievedItem.getLicenses().get(0).getName());
        }
      } else {
        Assert.assertEquals(msg + ": Status of collection should be unchanged", previousColStatus, retrivedCol.getStatus());
        if (item != null) {
          Assert.assertEquals(msg + "item status should be unchanged", previousItemStatus, retrievedItem.getStatus());
          if (expectedLicense != null) {
            Assert.assertEquals(msg + "item license name should be unchanged", expectedLicense.getName(),
                retrievedItem.getLicenses().get(0).getName());
          } else {
            Assert.assertEquals(msg + ": item should have no license", 0, retrievedItem.getLicenses().size());
          }
        }
      }
    } catch (ImejiException e) {
      Assert.fail(msg + ": " + e.getMessage());
    }
  }

  @Test
  public void withdraw() {
    CollectionService collectionService = new CollectionService();
    CollectionImeji collectionToWithdraw = ImejiFactory.newCollection().setTitle("To Withdraw").setPerson("m", "p", "g").build();
    collectionToWithdraw.setDiscardComment("discard test");
    try {
      collectionService.create(collectionToWithdraw, defaultUser);
      defaultUser = userService.retrieve(defaultUser.getId(), sysadmin);
      Item itemToWithdraw = ImejiFactory.newItem(collectionToWithdraw);
      new ItemService().create(itemToWithdraw, collectionToWithdraw, defaultUser);
      withdraw_Test("collection pending", collectionToWithdraw, defaultUser, WorkflowException.class);
      collectionService.release(collectionToWithdraw, defaultUser, getDefaultLicense());
      collectionToWithdraw = collectionService.retrieve(collectionToWithdraw.getId(), sysadmin);
      userEditGrant.getGrants().add(new Grant(GrantType.EDIT, collectionToWithdraw.getId().toString()).toGrantString());
      new UserService().update(userEditGrant, sysadmin);
      // withdraw_Test("user edit grant", collectionToWithdraw, userEditGrant,
      // NotAllowedError.class);

      collectionToWithdraw.setDiscardComment(null);
      withdraw_Test("No discard comment", collectionToWithdraw, defaultUser, UnprocessableError.class);
      collectionToWithdraw.setDiscardComment("discard test");
      withdraw_Test("released collection, collection admin user", collectionToWithdraw, defaultUser, null);
      collectionToWithdraw = collectionService.retrieve(collectionToWithdraw.getId(), defaultUser);
      withdraw_Test("collection already withdrawn", collectionToWithdraw, defaultUser, WorkflowException.class);

    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void withdraw_Test(String msg, CollectionImeji collection, User user, Class exception) {
    CollectionService service = new CollectionService();
    Status collectionPreviousStatus = collection.getStatus();
    try {
      service.withdraw(collection, user);
      if (exception != null) {
        Assert.fail(msg + ": no exception has been thrown");
      }
    } catch (ImejiException e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    try {
      CollectionImeji retrievedCol = service.retrieve(collection.getId(), sysadmin);

      if (exception == null) {
        Assert.assertEquals(msg + "collection status should be withdrawn", Status.WITHDRAWN, retrievedCol.getStatus());
        for (URI uri : collection.getImages()) {
          try {
            Item item = (new ItemService()).retrieve(uri.toString(), sysadmin);
            Assert.assertEquals(msg + "item status should be withdrawn", Status.WITHDRAWN, item.getStatus());
            Assert.assertEquals(msg + " item discard comment should be collection discard comment", collection.getDiscardComment(),
                item.getDiscardComment());
          } catch (ImejiException e) {
            Assert.fail(e.getMessage());
          }
        }
      } else {
        Assert.assertEquals(msg + ": Collection Status should still be unchanged", collectionPreviousStatus, retrievedCol.getStatus());
      }
    } catch (ImejiException e1) {
      Assert.fail(msg + ": " + e1.getMessage());
    }
  }

  @Test
  public void retrieveAll() {
    try {
      List<CollectionImeji> res = (new CollectionService()).retrieveAll();
      boolean containsPrivate = false;
      boolean containsReleased = false;
      for (CollectionImeji c : res) {
        if (c.getIdString().equals(collectionPrivate.getIdString())) {
          containsPrivate = true;
        }
        if (c.getIdString().equals(collectionReleased.getIdString())) {
          containsReleased = true;
        }
      }
      Assert.assertTrue("Both collections should be found", containsPrivate && containsReleased);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * True if the {@link Grant} is found in the collection
   *
   * @param grants
   * @param grant
   * @return
   */
  private boolean hasGrant(Collection<String> grants, Grant grant) {
    return grants != null && grants.contains(grant.toGrantString());
  }

  private String getLogoChecksum(CollectionImeji c) throws IOException, ImejiException {
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null, new File(TempFileUtil.getOrCreateTempDirectory().getCanonicalPath()));
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(c.getLogoUrl().toString(), fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

  /**
   * Get the instance default instance
   *
   * @return
   */
  private License getDefaultLicense() {
    final ImejiLicenses lic = StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getDefaultLicense()) ? ImejiLicenses.CC0
        : ImejiLicenses.valueOf(Imeji.CONFIG.getDefaultLicense());
    final License license = new License();
    license.setName(lic.name());
    license.setLabel(lic.getLabel());
    license.setUrl(lic.getUrl());
    return license;
  }

}
