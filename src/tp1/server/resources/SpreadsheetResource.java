package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.clients.SpreadsheetApiClient;
import tp1.clients.UsersApiClient;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.WebServiceType;
import tp1.util.Cell;
import tp1.util.InvalidCellIdException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static tp1.util.ExceptionMapper.throwWebAppException;

@WebService(
		serviceName = SoapSpreadsheets.NAME,
		targetNamespace = SoapSpreadsheets.NAMESPACE,
		endpointInterface = SoapSpreadsheets.INTERFACE
)
@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> spreadsheets;

	private final SpreadsheetEngine engine;

	private final UsersApiClient localUsersClient;

	public static Map<String, SpreadsheetApiClient> remoteSpreadsheetClients;

	private WebServiceType type;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(WebServiceType type, UsersApiClient localUsersClient, Map<String, SpreadsheetApiClient> remoteSpreadsheetClients) {
		this.type = type;
		this.localUsersClient = localUsersClient;
		this.remoteSpreadsheetClients = remoteSpreadsheetClients;
		this.spreadsheets = new HashMap<>();
		this.engine = SpreadsheetEngineImpl.getInstance();
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		if( sheet == null || password == null ) {
			throwWebAppException(Log, "Sheet or password null.", type, Response.Status.BAD_REQUEST);
		}

		String sheetId;
		do {
			sheetId = UUID.randomUUID().toString();
		} while (spreadsheets.containsKey(sheetId));

		synchronized(this) {

			//TODO: Evitar ciclos de referencias

			try {
				boolean valid = localUsersClient.verifyUser(sheet.getOwner(), password);
				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.BAD_REQUEST);
			} catch (Exception e) {
				throwWebAppException(Log, "User not found.", type, Response.Status.BAD_REQUEST);
			}

			spreadsheets.put(sheetId, sheet);
		}

		return sheetId;
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {

		if( sheetId == null || password == null ) {
			throwWebAppException(Log, "SheetId or password null.", type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
			}

			try {
				boolean valid = localUsersClient.verifyUser(sheet.getOwner(), password);
				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
			} catch (Exception e) {
				throwWebAppException(Log, "User not found.", type, Response.Status.BAD_REQUEST);
			}

			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(Log, "SheetId or userId or password null.", type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if( sheet == null ) {
			throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
		}

		if (!sheet.getSharedWith().contains(userId)) {
			throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.BAD_REQUEST);
		}

		if (!User.extractDomain(userId).equals(sheet.getOwnerDomain())) {
			throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.BAD_REQUEST);
		}

		try {
			boolean valid = localUsersClient.verifyUser(sheet.getOwner(), password);
			if(!valid)
				throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
		} catch (Exception e) {
			throwWebAppException(Log, "User not found.", type, Response.Status.NOT_FOUND);
		}

		return sheet;
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {

		Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throwWebAppException(Log, "Error in spreadsheet", type, Response.Status.BAD_REQUEST);
		}

		return result;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

		if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
			throwWebAppException(Log, "Malformed request.", type, Response.Status.BAD_REQUEST);
		}

		synchronized(this) {

			Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

			//TODO: Evitar ciclos de referencias

			try {
				Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

				spreadsheet.setCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);
			} catch (InvalidCellIdException e) {
				throwWebAppException(Log, "Invalid spreadsheet cell.", type, Response.Status.BAD_REQUEST);
			}

		}

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
}
