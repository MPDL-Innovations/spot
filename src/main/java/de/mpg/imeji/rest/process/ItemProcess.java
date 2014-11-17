package de.mpg.imeji.rest.process;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import de.mpg.imeji.logic.auth.Authentication;
import de.mpg.imeji.logic.auth.AuthenticationFactory;
import de.mpg.imeji.logic.auth.exception.NotAllowedError;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.api.ItemService;
import de.mpg.imeji.rest.to.ItemTO;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.j2j.exceptions.NotFoundException;

public class ItemProcess {

	public static JSONResponse readItem(HttpServletRequest req, String id) {
		JSONResponse resp = new JSONResponse();

		Authentication auth = AuthenticationFactory.factory(req);
		User u = auth.doLogin();

		Item item = null;
  
		ItemService icrud = new ItemService();
		try { 
			item = icrud.read(id, u);
			ItemTO to = new ItemTO();
			TransferObjectFactory.transferItem(item, to);
			resp.setObject(to);
			resp.setStatus(Status.OK);
		} catch (NotFoundException e) {
			resp.setObject(RestProcessUtils.buildBadRequestResponse());
			resp.setStatus(Status.BAD_REQUEST);

		} catch (NotAllowedError e) {
			if (u == null) {
				resp.setObject(RestProcessUtils.buildUnauthorizedResponse());
				resp.setStatus(Status.UNAUTHORIZED);
			} else {
				resp.setObject(RestProcessUtils.buildNotAllowedResponse());
				resp.setStatus(Status.FORBIDDEN);

			}
		} catch (Exception e) {

		}
		return resp;

	}
	


}