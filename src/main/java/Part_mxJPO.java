import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.common.util.ComponentsUIUtil;
import com.matrixone.apps.cpn.CPNCommon;
import com.matrixone.apps.cpn.util.ECMUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.CreateProcessCallable;
import com.matrixone.apps.framework.ui.PostProcessCallable;
import com.matrixone.apps.framework.ui.UIForm;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.manufacturerequivalentpart.Part;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class Part_mxJPO {
  private static final long serialVersionUID = 1L;
  
  final String POLICY_SUPPLIER_EQUIVALENT = PropertyUtil.getSchemaProperty("policy_SupplierEquivalent");
  
  final String POLICY_MANUFACTURER_EQUIVALENT = PropertyUtil.getSchemaProperty("policy_ManufacturerEquivalent");
  
  final String REL_PG_BUSINESSAPPROVER = PropertyUtil.getSchemaProperty("relationship_pgBusinessApprover");
  
  final String REL_PG_PRIMARY_ORGANIZATION = PropertyUtil.getSchemaProperty("relationship_pgPrimaryOrganization");
  
  final String STRING_CONST_ISMASSMEPSEP = "isMassMEPSEP";
  
  final String RANGE_VALUE_TRUE = "TRUE";
  
  public Part_mxJPO(Context context, String[] args) throws Exception {
    super(context, args);
  }
  
  @CreateProcessCallable
  public Map createCPCObject(Context context, String[] args) throws Exception {
    HashMap<String, String> requestMap = (HashMap)JPO.unpackArgs(args);
    Map<Object, Object> returnMap = new HashMap<>();
    try {
      UIForm uiForm = new UIForm();
      Locale localLocale = (Locale)requestMap.get("localeObj");
      requestMap.put("AutoNameSeries", "MEP");
      String objectId = uiForm.createObject(context, requestMap);
      returnMap.put("id", objectId);
    } catch (Exception e) {
      e.printStackTrace();
      throw new FrameworkException(e);
    } 
    return returnMap;
  }
  
  @PostProcessCallable
  public void createMfg(Context context, String[] args) throws Exception {
    String LOCATION_EQUIVALENT_OBJECT = "type_LocationEquivalentObject";
    String LOCATION_EQUIVALENT_POLICY = "policy_LocationEquivalent";
    String MANUFACTURER = "Manufacturer";
    String[] smepObjId = new String[0];
    try {
      DomainObject doEntPart = null;
      DomainObject doLocation = null;
      DomainObject doManufacturer = null;
      DomainObject doLocEquiv = null;
      DomainObject mepObject = null;
      String sLocEquivObjId = null;
      ContextUtil.startTransaction(context, true);
      HashMap programMap = (HashMap)JPO.unpackArgs(args);
      HashMap requestMap = (HashMap)programMap.get("requestMap");
      HashMap paramMap = (HashMap)programMap.get("paramMap");
      String sLocId = (String)requestMap.get("UsageLocation");
      String sLocPreference = (String)requestMap.get("Preference");
      String strBusinessApproverOID = (String)requestMap.get("BusinessApproverOID");
      String strOrganizationOID = (String)requestMap.get("OrganizationOID");
      String sLocStatus = (String)requestMap.get("Status");
      String smepId = (String)paramMap.get("objectId");
      smepObjId = new String[] { smepId };
      String manufacturerId = (String)requestMap.get("Manufacturer");
      String sManufacturerLocationId = (String)requestMap.get("ManufacturerLocation");
      String sEpId = (String)requestMap.get("objectId");
      if (manufacturerId != null && !manufacturerId.equals("null") && manufacturerId
        .length() > 0) {
        doManufacturer = new DomainObject(manufacturerId);
      } else {
        throw new MatrixException("emxManufacturerEquivalent.Part.InvalidManufacturer");
      } 
      Company contextComp = Person.getPerson(context).getCompany(context);
      StringList selectList = new StringList(2);
      selectList.addElement("name");
      selectList.addElement("id");
      Map compMap = contextComp.getInfo(context, selectList);
      String sDefaultLocId = (String)compMap.get("id");
      DomainObject doDefaultLocation = new DomainObject(sDefaultLocId);
      if (sLocId != null && sDefaultLocId.equals(sLocId))
        sLocId = null; 
      if (sLocId != null && !"null".equals(sLocId) && sLocId.length() > 0)
        doLocation = new DomainObject(sLocId); 
      if (smepId != null && !"null".equals(smepId) && smepId.length() > 0) {
        mepObject = DomainObject.newInstance(context, smepId);
        String strVendorTypeID = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump $5", new String[] { PropertyUtil.getSchemaProperty(context, "type_pgPLIVendorType"), "Manufacturer", "-", "id", "|" });
        if (UIUtil.isNotNullAndNotEmpty(strVendorTypeID))
          mepObject.addToObject(context, new RelationshipType(PropertyUtil.getSchemaProperty(context, "relationship_pgMEPtopgPLIVendorTypeForMEP")), strVendorTypeID); 
      } 
      if (sEpId != null && !sEpId.equals("null") && sEpId.length() > 0)
        doEntPart = new DomainObject(sEpId); 
      if (doManufacturer != null)
        mepObject.addRelatedObject(context, new RelationshipType(RELATIONSHIP_MANUFACTURING_RESPONSIBILITY), true, manufacturerId); 
      String sCompEngrRole = PropertyUtil.getSchemaProperty(context, "role_ComponentEngineer");
      if (doEntPart != null) {
        if (sLocId != null && !"null".equals(sLocId) && sLocId
          .length() > 0)
          sLocEquivObjId = FrameworkUtil.autoName(context, "type_LocationEquivalentObject", "", "policy_LocationEquivalent", null, null, false, false); 
        if (sLocEquivObjId != null)
          doLocEquiv = DomainObject.newInstance(context, sLocEquivObjId); 
        if (doDefaultLocation != null && doLocEquiv == null) {
          RelationshipType relAllocResp = new RelationshipType(RELATIONSHIP_ALLOCATION_RESPONSIBILITY);
          DomainRelationship domainRelationship = DomainRelationship.connect(context, doDefaultLocation, relAllocResp, mepObject);
        } 
        if (doLocEquiv != null) {
          RelationshipType relManuEquiv = new RelationshipType(RELATIONSHIP_MANUFACTURER_EQUIVALENT);
          DomainRelationship.connect(context, doLocEquiv, relManuEquiv, mepObject);
          if (doLocation != null) {
            RelationshipType relAllocResp = new RelationshipType(RELATIONSHIP_ALLOCATION_RESPONSIBILITY);
            DomainRelationship doRelationship = DomainRelationship.connect(context, doLocation, relAllocResp, doLocEquiv);
            doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_STATUS, sLocStatus);
            doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_PREFERENCE, sLocPreference);
          } 
        } 
        if (context.isAssigned(sCompEngrRole)) {
          if (sEpId != null && !"null".equalsIgnoreCase(sEpId) && 
            !"".equalsIgnoreCase(sEpId)) {
            if (doLocEquiv == null) {
              ContextUtil.pushContext(context);
              String conEpMEPCmd = "connect bus $1 relationship $2 from $3;";
              MqlUtil.mqlCommand(context, "history off");
              MqlUtil.mqlCommand(context, conEpMEPCmd, false, new String[] { mepObject.getInfo(context, "id"), RELATIONSHIP_MANUFACTURER_EQUIVALENT, sEpId });
              MqlUtil.mqlCommand(context, "history on");
              ContextUtil.popContext(context);
              String historyEpMEPCommand = "modify bus $1 add history $2 comment $3";
              MqlUtil.mqlCommand(context, historyEpMEPCommand, new String[] { sEpId, "connect", "connect " + RELATIONSHIP_MANUFACTURER_EQUIVALENT + "  to " + mepObject
                    .getInfo(context, "name") });
            } 
            ContextUtil.pushContext(context);
            if (doLocEquiv != null) {
              String connectLocCmd = "connect bus $1 relationship $2 from $3;";
              MqlUtil.mqlCommand(context, "history off");
              MqlUtil.mqlCommand(context, connectLocCmd, false, new String[] { doLocEquiv.getInfo(context, "id"), RELATIONSHIP_LOCATION_EQUIVALENT, sEpId });
              MqlUtil.mqlCommand(context, "history on");
              String historyCommand = "modify bus $1 add history $2 comment $3";
              MqlUtil.mqlCommand(context, historyCommand, new String[] { sEpId, "connect", "connect " + RELATIONSHIP_LOCATION_EQUIVALENT + "  to " + doLocEquiv.getInfo(context, "name") });
            } 
            ContextUtil.popContext(context);
          } 
        } else if (doLocEquiv != null) {
          doEntPart.addRelatedObject(context, new RelationshipType(RELATIONSHIP_LOCATION_EQUIVALENT), false, doLocEquiv
              
              .getInfo(context, "id"));
        } else {
          doEntPart.addRelatedObject(context, new RelationshipType(RELATIONSHIP_MANUFACTURER_EQUIVALENT), false, smepId);
        } 
        if (UIUtil.isNotNullAndNotEmpty(strBusinessApproverOID))
          DomainRelationship.connect(context, mepObject, this.REL_PG_BUSINESSAPPROVER, DomainObject.newInstance(context, strBusinessApproverOID)); 
        if (UIUtil.isNotNullAndNotEmpty(strOrganizationOID) && UIUtil.isNotNullAndNotEmpty(smepId))
          DomainRelationship.connect(context, smepId, this.REL_PG_PRIMARY_ORGANIZATION, strOrganizationOID, true); 
      } else {
        if (UIUtil.isNotNullAndNotEmpty(strBusinessApproverOID))
          DomainRelationship.connect(context, mepObject, this.REL_PG_BUSINESSAPPROVER, DomainObject.newInstance(context, strBusinessApproverOID)); 
        if (UIUtil.isNotNullAndNotEmpty(strOrganizationOID) && UIUtil.isNotNullAndNotEmpty(smepId))
          DomainRelationship.connect(context, smepId, this.REL_PG_PRIMARY_ORGANIZATION, strOrganizationOID, true); 
        if (doLocation != null) {
          RelationshipType relAllocResp = new RelationshipType(RELATIONSHIP_ALLOCATION_RESPONSIBILITY);
          DomainRelationship doRelationship = DomainRelationship.connect(context, doLocation, relAllocResp, mepObject);
          doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_STATUS, sLocStatus);
          doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_PREFERENCE, sLocPreference);
        } else {
          if (doDefaultLocation != null) {
            RelationshipType relAllocResp = new RelationshipType(RELATIONSHIP_ALLOCATION_RESPONSIBILITY);
            DomainRelationship doRelationship = DomainRelationship.connect(context, doDefaultLocation, relAllocResp, mepObject);
            if (sLocStatus != null) {
              doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_STATUS, sLocStatus);
              doRelationship.setAttributeValue(context, ATTRIBUTE_LOCATION_PREFERENCE, sLocPreference);
            } 
          } 
          String changeTemplateId = (String)requestMap.get("ChangeTemplateOID");
          String coId = (String)requestMap.get("COOID");
          String strOrgNameOfProductData = mepObject.getInfo(context, "organization");
          String strRDOID = CPNCommon.getIDForName(context, DomainConstants.TYPE_ORGANIZATION, strOrgNameOfProductData);
          Map<Object, Object> tempParamMap = new HashMap<>();
          tempParamMap.put("changeTemplate", changeTemplateId);
          tempParamMap.put("co", coId);
          tempParamMap.put("rdoId", strRDOID);
          tempParamMap.put("strProdDataId", smepId);
          if (UIUtil.isNotNullAndNotEmpty(changeTemplateId))
            tempParamMap.put("changeTemplate", changeTemplateId); 
          if (UIUtil.isNotNullAndNotEmpty(smepId) && (UIUtil.isNotNullAndNotEmpty(changeTemplateId) || (UIUtil.isNotNullAndNotEmpty(coId) && FrameworkUtil.isObjectId(context, coId))))
            ECMUtil.createAndConnectCO(context, JPO.packArgs(tempParamMap)); 
        } 
      } 
      if (sManufacturerLocationId != null && 
        !"null".equalsIgnoreCase(sManufacturerLocationId) && 
        !"".equals(sManufacturerLocationId)) {
        String relManufacturingLocation = PropertyUtil.getSchemaProperty(context, "relationship_ManufacturingLocation");
        mepObject.addToObject(context, new RelationshipType(relManufacturingLocation), sManufacturerLocationId);
      } 
      String sEndItem = mepObject.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_EndItem"));
      if ("Yes".equals(sEndItem)) {
        mepObject.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_EndItemOverrideEnabled"), "No");
      } else {
        mepObject.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_EndItemOverrideEnabled"), "Yes");
      } 
      mepObject.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_isVPMVisible"), "FALSE");
      ContextUtil.commitTransaction(context);
    } catch (Exception e) {
      e.printStackTrace();
      try {
        ContextUtil.abortTransaction(context);
        Part.deleteMEPs(context, smepObjId);
      } catch (Exception ex) {
        throw new Exception("MEP not created.Please create again with valid data");
      } 
      throw new FrameworkException(e);
    } 
  }
  
  public void addApproverTask(Context context, String[] args) throws Exception {
    try {
      int nReturnCode = 0;
      String strParentObjId = args[0];
      String strParentObjectState = args[1];
      String StrApproverType = args[2];
      String strApprover = args[3];
      String strInstructions = args[4];
      String strTitle = args[5];
      String strRouteAction = args[6];
      String strDueDateOption = args[7];
      String strDueDate = args[8];
      String strDueDateOffset = args[9];
      String strDueDateOffsetFrom = args[10];
      String strAllowDelegation = args[11];
      String strRequiresOwnerReview = args[12];
      StringList slRouteIdsToResume = new StringList();
      String strRouteIdToResume = "";
      String ATTRIBUTE_ROUTE_COMPLETION_ACTION = PropertyUtil.getSchemaProperty(context, "attribute_RouteCompletionAction");
      String PROMOTE_CONNECTED_OBJECT = "Promote Connected Object";
      String ROUTE_STATUS_FINISHED = "Finished";
      if ("true".equalsIgnoreCase(strAllowDelegation)) {
        strAllowDelegation = "TRUE";
      } else {
        strAllowDelegation = "FALSE";
      } 
      if ("true".equalsIgnoreCase(strRequiresOwnerReview)) {
        strRequiresOwnerReview = "Yes";
      } else {
        strRequiresOwnerReview = "No";
      } 
      String strLanguage = context.getSession().getLanguage();
      i18nNow loc = new i18nNow();
      String RESOURCE_BUNDLE = "emxFrameworkStringResource";
      String ROUTE_FINISHED = loc.GetString("emxFrameworkStringResource", strLanguage, "emxFramework.Range.Route_Status.Finished");
      String COMPLETED_ROUTE = loc.GetString("emxFrameworkStringResource", strLanguage, "emxFramework.Alert.CannotAddTaskToCompletedRoute");
      String SELECT_ROUTE_TASK_ASSIGNEE_TYPE = "from[" + DomainObject.RELATIONSHIP_PROJECT_TASK + "].to.type";
      String SELECT_ATTRIBUTE_ROUTE_STATUS = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_STATUS + "]";
      String ATTRIBUTE_CURRENT_ROUTE_NODE = PropertyUtil.getSchemaProperty(context, "attribute_CurrentRouteNode");
      String SELECT_ATTRIBUTE_CURRENT_ROUTE_NODE = "attribute[" + ATTRIBUTE_CURRENT_ROUTE_NODE + "]";
      String STR_NOTSTARTED = "Not Started";
      String STR_STARTED = "Started";
      String STR_STOPPED = "Stopped";
      String STR_PERSON = "Person";
      String STR_ROLE = "Role";
      String STR_GROUP = "Group";
      StringList slBusSelect = new StringList();
      slBusSelect.add("policy");
      slBusSelect.add("current");
      slBusSelect.add("from[" + this.REL_PG_BUSINESSAPPROVER + "].to.id");
      slBusSelect.add("name");
      DomainObject dmoParentObject = new DomainObject(strParentObjId);
      Map mapObjectInfo = dmoParentObject.getInfo(context, slBusSelect);
      String strParentObjectName = (String)mapObjectInfo.get("name");
      String strParentObjectPolicy = (String)mapObjectInfo.get("policy");
      String strParentObjectCurrentState = (String)mapObjectInfo.get("current");
      strApprover = (String)mapObjectInfo.get("from[" + this.REL_PG_BUSINESSAPPROVER + "].to.id");
      String strSymbolicObjectPolicyName = FrameworkUtil.getAliasForAdmin(context, "Policy", strParentObjectPolicy, false);
      String strSymbolicParentObjectState = FrameworkUtil.reverseLookupStateName(context, strParentObjectPolicy, strParentObjectState);
      StringList slRelSelect = new StringList(DomainRelationship.ATTRIBUTE_ROUTE_SEQUENCE);
      String SELECT_ATTRIBUTE_ROUTE_TEMPLATE_TASK_EDIT_SETTING = "from[" + DomainConstants.RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + "].to.attribute[" + DomainConstants.ATTRIBUTE_TASKEDIT_SETTING + "]";
      short nRecurseToLevel = 1;
      slBusSelect = new StringList();
      slBusSelect.add("id");
      slBusSelect.add(SELECT_ROUTE_TASK_ASSIGNEE_TYPE);
      slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_STATUS);
      slBusSelect.add(SELECT_ATTRIBUTE_CURRENT_ROUTE_NODE);
      slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_TEMPLATE_TASK_EDIT_SETTING);
      slBusSelect.add("name");
      String strRelWhere = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_BASE_POLICY + "]=='" + strSymbolicObjectPolicyName + "' && attribute[" + DomainObject.ATTRIBUTE_ROUTE_BASE_STATE + "]=='" + strSymbolicParentObjectState + "'";
      String strObjectWhere = "";
      String strRouteSequence = null;
      String strRouteId = null;
      String strPersonObjId = null;
      String strRouteStatus = null;
      String strRouteTaskUser = "";
      String strRelPattern = DomainObject.RELATIONSHIP_OBJECT_ROUTE;
      String strTypePattern = DomainObject.TYPE_ROUTE;
      Map mapRouteInfo = null;
      Map mapStartedRoute = null;
      Map mapNotStartedRoute = null;
      Map mapStoppededRoute = null;
      Map mapTemp = null;
      Map<Object, Object> mapRelRouteNodeAttributes = new HashMap<>();
      Map mapRouteToBeUsed = null;
      MapList mlTemp = new MapList();
      DomainRelationship dmrRouteNode = null;
      boolean GET_TO = false;
      boolean GET_FROM = true;
      boolean isNewRouteCreated = false;
      String RANGE_APPROVAL = "Approval";
      Route route = (Route)DomainObject.newInstance(context, DomainConstants.TYPE_ROUTE);
      MapList mlRoutes = dmoParentObject.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, false, true, nRecurseToLevel, strObjectWhere, strRelWhere);
      Map mapTempRoute = null;
      String sRouteId = "";
      String sRouteStatus = "";
      String sRouteName = "";
      StringList slRouteIdsToDelete = new StringList();
      String[] saRoutesToDelete = null;
      boolean isContextPushed = false;
      if (mlRoutes != null && mlRoutes.size() > 0) {
        Iterator<Map> tempItr = mlRoutes.iterator();
        while (tempItr.hasNext()) {
          mapTempRoute = tempItr.next();
          sRouteId = (String)mapTempRoute.get("id");
          sRouteStatus = (String)mapTempRoute.get(SELECT_ATTRIBUTE_ROUTE_STATUS);
          sRouteName = (String)mapTempRoute.get("name");
          if (UIUtil.isNotNullAndNotEmpty(sRouteStatus) && "Stopped".equalsIgnoreCase(sRouteStatus) && sRouteName.contains(strParentObjectName)) {
            slRouteIdsToDelete.add(sRouteId);
            continue;
          } 
          if (!ROUTE_STATUS_FINISHED.equalsIgnoreCase(sRouteStatus) || !sRouteName.contains(strParentObjectName))
            slRouteIdsToResume.add(sRouteId); 
        } 
      } 
      if ((this.POLICY_MANUFACTURER_EQUIVALENT.equalsIgnoreCase(strParentObjectPolicy) || this.POLICY_SUPPLIER_EQUIVALENT.equalsIgnoreCase(strParentObjectPolicy)) && slRouteIdsToDelete.size() > 0) {
        saRoutesToDelete = new String[slRouteIdsToDelete.size()];
        slRouteIdsToDelete.toArray((Object[])saRoutesToDelete);
        JPO.invoke(context, "pgRoute", null, "deleteRoutes", saRoutesToDelete);
        slRouteIdsToDelete.clear();
      } 
      if (slRouteIdsToDelete.size() == 0) {
        String strRouteName = FrameworkUtil.autoName(context, 
            FrameworkUtil.getAliasForAdmin(context, "type", DomainConstants.TYPE_ROUTE, true), (new Policy(DomainObject.POLICY_ROUTE))
            .getFirstInSequence(context), 
            FrameworkUtil.getAliasForAdmin(context, "policy", DomainConstants.POLICY_ROUTE, true), null, null, true, true);
        String strRouteFullname = strRouteName + "_" + strParentObjectName;
        route.createObject(context, DomainConstants.TYPE_ROUTE, strRouteFullname, null, DomainObject.POLICY_ROUTE, null);
        String strPersonOwner = dmoParentObject.getOwner(context).getName();
        String strPersonOwnerId = PersonUtil.getPersonObjectID(context, strPersonOwner);
        route.addRelatedObject(context, new RelationshipType(DomainObject.RELATIONSHIP_PROJECT_ROUTE), false, strPersonOwnerId);
        HashMap<Object, Object> mapState = new HashMap<>();
        mapState.put(strParentObjId, strParentObjectState);
        route.AddContent(context, new String[] { strParentObjId }, mapState);
        strRouteId = route.getId();
        isNewRouteCreated = true;
        if ("Person".equals(StrApproverType)) {
          strPersonObjId = strApprover;
          dmrRouteNode = DomainRelationship.connect(context, (DomainObject)route, DomainObject.RELATIONSHIP_ROUTE_NODE, new DomainObject(strPersonObjId));
        } else if ("Role".equals(StrApproverType) || "Group".equals(StrApproverType)) {
          DomainObject dmoRTU = DomainObject.newInstance(context);
          dmrRouteNode = dmoRTU.createAndConnect(context, DomainObject.TYPE_ROUTE_TASK_USER, DomainObject.RELATIONSHIP_ROUTE_NODE, (DomainObject)route, true);
          strRouteTaskUser = FrameworkUtil.getAliasForAdmin(context, StrApproverType, strApprover, true);
        } else {
          String[] formatArgs = { StrApproverType };
          String message = ComponentsUIUtil.getI18NString(context, "emxComponents.LifeCycle.InvalidApproverType", formatArgs);
          throw new Exception(message);
        } 
        mapRelRouteNodeAttributes = new HashMap<>();
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ROUTE_TASK_USER, strRouteTaskUser);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ROUTE_INSTRUCTIONS, strInstructions);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ROUTE_ACTION, strRouteAction);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_TITLE, strTitle);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ALLOW_DELEGATION, strAllowDelegation);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_REVIEW_TASK, strRequiresOwnerReview);
        mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "No");
        if ("assigneeSetDueDateSet".equals(strDueDateOption)) {
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "Yes");
        } else if ("dueDateSet".equals(strDueDateOption)) {
          SimpleDateFormat dateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), Locale.US);
          String strDueDateToSet = dateFormat.format(new Date(Long.parseLong(strDueDate)));
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strDueDateToSet);
        } else if ("dueDateOffsetSet".equals(strDueDateOption)) {
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_DUEDATE_OFFSET, strDueDateOffset);
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_DATE_OFFSET_FROM, strDueDateOffsetFrom);
        } 
        if (isNewRouteCreated == true) {
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ROUTE_SEQUENCE, "1");
        } else {
          if (strRouteSequence == null || "".equals(strRouteSequence) || "null".equals(strRouteSequence))
            strRouteSequence = "1"; 
          mapRelRouteNodeAttributes.put(DomainObject.ATTRIBUTE_ROUTE_SEQUENCE, strRouteSequence);
        } 
        dmrRouteNode.setAttributeValues(context, mapRelRouteNodeAttributes);
        route.setAttributeValue(context, DomainObject.ATTRIBUTE_ROUTE_BASE_PURPOSE, "Approval");
        route.setAttributeValue(context, ATTRIBUTE_ROUTE_COMPLETION_ACTION, "Promote Connected Object");
        if (strParentObjectCurrentState.equals(strParentObjectState) && isNewRouteCreated == true) {
          route.promote(context);
          route.setDueDateFromOffsetForGivenLevelTasks(context, 1);
        } 
        Object objectRouterelObj = route.getInfo(context, "to[" + DomainObject.RELATIONSHIP_OBJECT_ROUTE + "].id");
        String objectRouteRelId = "";
        if (objectRouterelObj instanceof String) {
          objectRouteRelId = (String)objectRouterelObj;
          DomainRelationship domRel = DomainRelationship.newInstance(context, objectRouteRelId);
          ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
          domRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, "Approval");
          ContextUtil.popContext(context);
        } 
        if ("Started".equals(strRouteStatus)) {
          route.startTasksOnCurrentLevel(context);
        } else if ("Stopped".equals(strRouteStatus)) {
          route.setAttributeValue(context, DomainObject.ATTRIBUTE_ROUTE_STATUS, "Started");
          route.startTasksOnCurrentLevel(context);
        } 
      } 
      try {
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
        isContextPushed = true;
        if (null != slRouteIdsToResume && !slRouteIdsToResume.isEmpty())
          for (int j = 0; j < slRouteIdsToResume.size(); j++) {
            strRouteIdToResume = (String)slRouteIdsToResume.get(j);
            route = new Route(strRouteIdToResume);
            route.resume(context);
          }  
      } catch (Exception ex) {
        ex.printStackTrace();
        throw ex;
      } finally {
        if (isContextPushed)
          ContextUtil.popContext(context); 
      } 
    } catch (Exception exp) {
      exp.printStackTrace();
      throw exp;
    } 
  }
  
  @PostProcessCallable
  public void connectMEPToEPPostProcess(Context context, String[] args) throws Exception {
    HashMap programMap = (HashMap)JPO.unpackArgs(args);
    HashMap requestMap = (HashMap)programMap.get("requestMap");
    HashMap paramMap = (HashMap)programMap.get("paramMap");
    String strPartId = (String)paramMap.get("newObjectId");
    String strObjectId = (String)requestMap.get("objectId");
    String strRelSymbolicName = (String)requestMap.get("relName");
    String strRelName = PropertyUtil.getSchemaProperty(context, strRelSymbolicName);
    DomainObject productDataObj = null;
    if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strPartId)) {
      productDataObj = DomainObject.newInstance(context, strPartId);
      DomainRelationship domainRelationship = DomainRelationship.connect(context, productDataObj, strRelName, new DomainObject(strObjectId));
    } 
  }
}
