package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> spreadsheets = new HashMap<>();

	private final RestUsers users = null;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(UsersResource users) {
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		String sheetId = sheet.getSheetId();

		checkSheetIdString(sheetId);

		synchronized(this) {

			users.getUser(sheet.getOwner(), password);

			spreadsheets.put(sheetId, sheet);
		}

		return sheetId;
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {

		checkSheetIdString(sheetId);

		synchronized (this) {

			Spreadsheet sheet = checkSheet(sheetId);

			users.getUser(sheet.getOwner(), password);

			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		checkSheetIdString(sheetId);

		synchronized (this) {

			Spreadsheet sheet = checkSheet(sheetId);

			String ownerId = sheet.getOwner();

			User requestUser = users.getUser(userId, password);

			if (ownerId.equals(userId)
				|| (sheet.getSharedWith().contains(userId)
					&& sheet.getOwnerDomain().equals(requestUser.extractDomain())))
				return sheet;

			Log.info("User '" + userId + "' does not have permissions to read this spreadsheet.");
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {

		// TODO

		return new String[0][];
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

		checkSheetIdString(sheetId);




	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {

		checkSheetIdString(sheetId);

		synchronized (this) {

			Spreadsheet sheet = checkSheet(sheetId);

			users.getUser(sheet.getOwner(), password);

			Set<String> sharedWith = sheet.getSharedWith();

			if (sharedWith.contains(userId)) {
				Log.info("Spreadsheet is already being shared with this user.");
				throw new WebApplicationException(Response.Status.CONFLICT);
			}

			sharedWith.add(userId);
		}

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {

		checkSheetIdString(sheetId);

		synchronized (this) {

			Spreadsheet sheet = checkSheet(sheetId);

			users.getUser(sheet.getOwner(), password);

			Set<String> sharedWith = sheet.getSharedWith();

			if (!sharedWith.contains(userId)) {
				Log.info("Spreadsheet is already being shared with this user.");
				throw new WebApplicationException(Response.Status.NOT_FOUND);
			}

			sharedWith.remove(userId);
		}
	}

	private void checkSheetIdString(String sheetId) {

		if (sheetId == null) {
			Log.info("Sheet is null.");
			throw new WebApplicationException(Response.Status.BAD_REQUEST); //400
		}
	}

	private Spreadsheet checkSheet(String sheetId) {

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if (sheet == null ) {
			Log.info("No sheet exists with the given sheetId.");
			throw new WebApplicationException(Response.Status.NOT_FOUND); //404
		}

		return sheet;
	}
}
