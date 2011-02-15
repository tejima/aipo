/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowFile;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequest;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRequestMap;
import com.aimluck.eip.cayenne.om.portlet.EipTWorkflowRoute;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.whatsnew.util.WhatsNewUtils;
import com.aimluck.eip.workflow.util.WorkflowUtils;
import com.aimluck.eip.workflow.util.WorkflowUtils.Type;

/**
 * ワークフローのフォームデータを管理するクラスです。 <BR>
 * 
 */
public class WorkflowFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(WorkflowFormData.class.getName());

  /** Request名 */
  private ALStringField request_name;

  /** カテゴリID */
  private ALNumberField category_id;

  /** 申請経路ID */
  private ALNumberField route_id;

  private ALStringField route;

  /** 重要度 */
  private ALNumberField priority;

  /** メモ */
  private ALStringField note;

  /** 金額 */
  private ALNumberField price;

  /** カテゴリ一覧 */
  private List<WorkflowCategoryResultData> categoryList;

  /** 申請経路一覧 */
  private List<WorkflowRouteResultData> routeList;

  /** 申請先ユーザIDリスト */
  private ALStringField positions;

  /** 申請先一覧 */
  private List<ALEipUser> memberList;

  /** ファイルアップロードリスト */
  private List<FileuploadLiteBean> fileuploadList;

  /** 添付フォルダ名 */
  private String folderName = null;

  private String orgId;

  private int uid;

  /** */
  private boolean is_saved_route;

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * 
   * 
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    login_user = ALEipUtils.getALEipUser(rundata);

    uid = ALEipUtils.getUserId(rundata);
    orgId = Database.getDomainName();
    folderName = rundata.getParameters().getString("folderName");
  }

  /**
   * 各フィールドを初期化します。 <BR>
   * 
   * 
   */
  @Override
  public void initField() {
    // リクエスト名
    request_name = new ALStringField();
    request_name.setFieldName("表題");
    request_name.setTrim(true);
    // カテゴリID
    category_id = new ALNumberField();
    category_id.setFieldName("カテゴリ");
    // 申請経路ID
    route_id = new ALNumberField();
    route_id.setFieldName("申請経路");
    // 申請経路
    route = new ALStringField();
    request_name.setTrim(true);
    // 重要度
    priority = new ALNumberField(3);
    priority.setFieldName("重要度");
    // メモ
    note = new ALStringField();
    note.setFieldName("申請内容");
    note.setTrim(false);

    // 金額
    price = new ALNumberField();
    price.setFieldName("金額");
    // 申請先のリスト
    positions = new ALStringField();
    positions.setFieldName("ユーザ名リスト");
    positions.setTrim(true);
    // 申請先一覧
    memberList = new ArrayList<ALEipUser>();
    // ファイルリスト
    fileuploadList = new ArrayList<FileuploadLiteBean>();
  }

  /**
   * 
   * @param rundata
   * @param context
   */
  public void loadCategoryList(RunData rundata, Context context) {
    categoryList = WorkflowUtils.loadCategoryList(rundata, context);
  }

  /**
   * 
   * @param rundata
   * @param context
   */
  public void loadRouteList(RunData rundata, Context context) {
    routeList = WorkflowUtils.loadRouteList(rundata, context);
  }

  /**
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);
    if (res) {
      try {
        String mode = rundata.getParameters().getString(ALEipConstants.MODE);
        if (ALEipConstants.MODE_INSERT.equals(mode)
          || ALEipConstants.MODE_UPDATE.equals(mode)) {

          is_saved_route = rundata.getParameters().getBoolean("is_saved_route");
          String userNames[] = rundata.getParameters().getStrings("positions");
          if (is_saved_route) {

            SelectQuery<EipTWorkflowRoute> query1 =
              Database.query(EipTWorkflowRoute.class);
            Expression exp1 =
              ExpressionFactory.matchDbExp(
                EipTWorkflowRoute.ROUTE_ID_PK_COLUMN,
                route_id.getValue());

            query1.setQualifier(exp1);

            List<EipTWorkflowRoute> list1 = query1.fetchList();

            EipTWorkflowRoute record = list1.get(0);
            String route = record.getRoute();

            if (!"".equals(route)) {
              StringTokenizer st = new StringTokenizer(route, ",");
              int lengthru = st.countTokens();
              String routeUserNames[] = new String[lengthru];

              for (int i = 0; i < lengthru; i++) {
                routeUserNames[i] = WorkflowUtils.getUserName(st.nextToken());
              }

              SelectQuery<TurbineUser> query =
                Database.query(TurbineUser.class);
              Expression exp2 =
                ExpressionFactory.inExp(
                  TurbineUser.LOGIN_NAME_PROPERTY,
                  routeUserNames);
              query.setQualifier(exp2);

              List<TurbineUser> list = query.fetchList();

              TurbineUser record1 = null;
              int length = routeUserNames.length;
              int enableUserNum = 0;
              for (int i = 0; i < length; i++) {
                record1 = getEipUserRecord(list, routeUserNames[i]);
                if ("F".equals(record1.getDisabled())) {
                  enableUserNum++;
                }
                ALEipUser user = new ALEipUser();
                user.initField();
                user.setUserId(record1.getUserId().intValue());
                user.setName(record1.getLoginName());
                user
                  .setAliasName(record1.getFirstName(), record1.getLastName());
                memberList.add(user);
              }

              // 有効なユーザーが一人もいなかった場合はダミーデータを入れる(validationのため)
              if (enableUserNum == 0) {
                memberList.clear();
                memberList.add(null);
              }
            }
          } else if (userNames != null && userNames.length > 0) {

            SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
            Expression exp1 =
              ExpressionFactory.inExp(
                TurbineUser.LOGIN_NAME_PROPERTY,
                userNames);
            Expression exp2 =
              ExpressionFactory.matchExp(TurbineUser.DISABLED_PROPERTY, "F");
            query.setQualifier(exp1);
            query.andQualifier(exp2);

            List<TurbineUser> list = query.fetchList();

            TurbineUser record = null;
            int length = userNames.length;
            for (int i = 0; i < length; i++) {
              record = getEipUserRecord(list, userNames[i]);
              ALEipUser user = new ALEipUser();
              user.initField();
              user.setUserId(record.getUserId().intValue());
              user.setName(record.getLoginName());
              user.setAliasName(record.getFirstName(), record.getLastName());
              memberList.add(user);
            }
          }
          fileuploadList = FileuploadUtils.getFileuploadList(rundata);
        }
      } catch (Exception ex) {
        logger.error("Exception", ex);
      }

      category_id.getValue();

      String event[] = rundata.getParameters().getStrings("event");
      if (event != null) {
        Iterator<WorkflowCategoryResultData> i = categoryList.iterator();
        while (i.hasNext()) {
          WorkflowCategoryResultData tmp = i.next();
          if (tmp.getCategoryId().getValue() == category_id.getValue()) {
            note.setValue(tmp.getOrderTemplate().toString());
          }
        }
      }
    }
    return res;
  }

  /**
   * テンプレートを渡す
   * 
   * @param num
   * @return
   */
  public String categoryTemplate(int num) {
    // Iterator i = categoryList.iterator();
    for (Object o : categoryList) {
      WorkflowCategoryResultData tmp = (WorkflowCategoryResultData) o;
      if (tmp.getCategoryId().getValue() == num) {
        return tmp.getOrderTemplate().getValue();
      }
    }

    return "";
  }

  /**
   * 指定したユーザ名のオブジェクトを取得する．
   * 
   * @param userList
   * @param userName
   * @return
   */
  private TurbineUser getEipUserRecord(List<TurbineUser> userList,
      String userName) {
    int size = userList.size();
    for (int i = 0; i < size; i++) {
      TurbineUser record = userList.get(i);
      if (record.getLoginName().equals(userName)) {
        return record;
      }
    }
    return null;
  }

  /**
   * リクエストの各フィールドに対する制約条件を設定します。 <BR>
   * 
   * 
   */
  @Override
  protected void setValidator() {
    // リクエスト名の文字数制限
    request_name.limitMaxLength(50);
    // 金額
    price.limitMaxValue(1000000000);
    // メモの文字数制限
    note.setNotNull(true);
    note.limitMaxLength(1000);
  }

  /**
   * リクエストのフォームに入力されたデータの妥当性検証を行います。 <BR>
   * 
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * 
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // リクエスト名
    request_name.validate(msgList);
    // メモ
    note.validate(msgList);
    // 金額
    price.validate(msgList);

    // 申請先
    if (memberList == null || memberList.size() <= 0) {
      msgList.add("『 <span class='em'>申請先</span> 』を指定してください。");
    } else if (!(memberList.get(0) instanceof ALEipUser)) {
      msgList.add("申請先のユーザーが全て無効、もしくは削除されています。有効なユーザーが一人以上いる経路を選択してください。");
    }

    return (msgList.size() == 0);

  }

  /**
   * リクエストをデータベースから読み出します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTWorkflowRequest request =
        WorkflowUtils.getEipTWorkflowRequestForOwner(rundata, context);
      if (request == null) {
        return false;
      }

      // リクエスト名
      request_name.setValue(request.getRequestName());
      // カテゴリID
      category_id.setValue(request
        .getEipTWorkflowCategory()
        .getCategoryId()
        .longValue());
      // 申請経路ID
      if (request.getEipTWorkflowRoute() != null) {
        is_saved_route = true;
        route_id.setValue(request
          .getEipTWorkflowRoute()
          .getRouteId()
          .longValue());
        route.setValue(request.getEipTWorkflowRoute().getRoute());
      }
      // 優先度
      priority.setValue(request.getPriority().longValue());
      // メモ
      note.setValue(request.getNote());
      // 金額
      price.setValue(request.getPrice().longValue());

      List<EipTWorkflowRequestMap> maps =
        WorkflowUtils.getEipTWorkflowRequestMap(request);
      EipTWorkflowRequestMap map = null;
      int size = maps.size();
      for (int i = 0; i < size; i++) {
        map = maps.get(i);
        int user_id = map.getUserId().intValue();
        if (!WorkflowUtils.DB_STATUS_REQUEST.equals(map.getStatus())) {
          memberList.add(ALEipUtils.getALEipUser(user_id));
        }
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * リクエストをデータベースから削除します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTWorkflowRequest request =
        WorkflowUtils.getEipTWorkflowRequestAll(rundata, context);
      int userId = ALEipUtils.getUserId(rundata);

      if (!request.getUserId().equals(userId)) {
        // 自分のワークフローではない場合、そのワークフローが自分より前に差し戻せるか確かめる
        List<EipTWorkflowRequestMap> requestMapList =
          WorkflowUtils.getEipTWorkflowRequestMap(request);
        EipTWorkflowRequestMap requestMap;
        TurbineUser requestMapUser;
        int listLength = requestMapList.size();
        for (int i = 0; i < listLength; i++) {
          requestMap = requestMapList.get(i);
          if (requestMap.getUserId().intValue() == userId) {
            break;
          }

          // 自分より前に差し戻せるなら、削除させない
          requestMapUser =
            WorkflowUtils.getTurbineUser(requestMap.getUserId().toString());
          if ("F".equals(requestMapUser.getDisabled())) {
            return false;
          }
        }
      }

      // イベントログ用
      String catname = request.getEipTWorkflowCategory().getCategoryName();
      String reqname = request.getRequestName();

      // ファイル削除処理
      List<String> fpaths = new ArrayList<String>();
      SelectQuery<EipTWorkflowFile> query =
        Database.query(EipTWorkflowFile.class);
      query.andQualifier(ExpressionFactory.matchDbExp(
        EipTWorkflowFile.EIP_TWORKFLOW_REQUEST_PROPERTY,
        request.getRequestId()));
      List<EipTWorkflowFile> files = query.fetchList();
      if (files != null && files.size() > 0) {
        int fsize = files.size();
        for (int j = 0; j < fsize; j++) {
          fpaths.add((files.get(j)).getFilePath());
        }
      }

      if (fpaths.size() > 0) {
        // ローカルファイルに保存されているファイルを削除する．
        File file = null;
        int fsize = fpaths.size();
        for (int i = 0; i < fsize; i++) {
          file =
            new File(WorkflowUtils.getSaveDirPath(orgId, uid) + fpaths.get(i));
          if (file.exists()) {
            file.delete();
          }
        }
      }

      // リクエストを削除
      Database.delete(request);
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        request.getRequestId(),
        ALEventlogConstants.PORTLET_TYPE_WORKFLOW,
        catname + " " + reqname);

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * リクエストをデータベースに格納します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      Date nowDate = Calendar.getInstance().getTime();

      EipTWorkflowCategory category =
        WorkflowUtils.getEipTWorkflowCategory(Long.valueOf(category_id
          .getValue()));

      // 新規オブジェクトモデル
      EipTWorkflowRequest request = Database.create(EipTWorkflowRequest.class);

      // リクエスト名
      request.setRequestName(request_name.getValue());
      // 親ID
      request.setParentId(Integer.valueOf(0));
      // カテゴリID
      request.setEipTWorkflowCategory(category);
      // 申請経路ID
      if (is_saved_route) {
        EipTWorkflowRoute route =
          WorkflowUtils.getEipTWorkflowRoute(Long.valueOf(route_id.getValue()));
        request.setEipTWorkflowRoute(route);
      }
      // ユーザーID
      request.setUserId(Integer
        .valueOf((int) login_user.getUserId().getValue()));
      // 優先度
      request.setPriority(Short.valueOf((short) priority.getValue()));
      // メモ
      request.setNote(note.getValue());
      // 金額
      request.setPrice(Long.valueOf(price.getValue()));
      // 承認フラグ
      request.setProgress(WorkflowUtils.DB_PROGRESS_WAIT);
      // 作成日
      request.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      request.setUpdateDate(Calendar.getInstance().getTime());

      // 申請先の設定
      insertEipTWorkflowRequestMap(
        request,
        login_user,
        WorkflowUtils.DB_STATUS_REQUEST,
        0,
        nowDate);

      // 申請先が削除・無効化されていたら次へまわす
      int size = memberList.size();
      ALEipUser toUser;
      int i;
      for (i = 0; i < size; i++) {
        toUser = memberList.get(i);
        if (WorkflowUtils.getUserIsDisabledOrDeleted(toUser
          .getUserId()
          .getValueAsString())) {
          insertEipTWorkflowRequestMap(
            request,
            toUser,
            WorkflowUtils.DB_STATUS_THROUGH,
            i + 1,
            nowDate);
        } else {
          // 0から数えてi番目のユーザーが確認中
          insertEipTWorkflowRequestMap(
            request,
            toUser,
            WorkflowUtils.DB_STATUS_CONFIRM,
            (i + 1),
            nowDate);
          break;
        }
      }

      // i + 1番目のユーザー以降の状態を「確認前」状態にする
      for (int j = i + 1; j < size; j++) {
        insertEipTWorkflowRequestMap(
          request,
          memberList.get(j),
          WorkflowUtils.DB_STATUS_WAIT,
          (j + 1),
          nowDate);
      }

      // 添付ファイル処理
      if (!WorkflowUtils.insertFileDataDelegate(
        rundata,
        context,
        request,
        null,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      // リクエストを登録
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        request.getRequestId(),
        ALEventlogConstants.PORTLET_TYPE_WORKFLOW,
        request.getEipTWorkflowCategory().getCategoryName()
          + " "
          + request_name.getValue());

      // 削除・無効化されていない最初の申請先に新着ポートレット登録
      ALEipUser nextUser = memberList.get(i);
      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

      if (aclhandler.hasAuthority(
        (int) nextUser.getUserId().getValue(),
        ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF,
        ALAccessControlConstants.VALUE_ACL_DETAIL)) {
        WhatsNewUtils.insertWhatsNew(
          WhatsNewUtils.WHATS_NEW_TYPE_WORKFLOW_REQUEST,
          request.getRequestId().intValue(),
          (int) nextUser.getUserId().getValue());
      }

      // アクティビティ
      List<String> recipients = new ArrayList<String>();
      recipients.add(nextUser.getName().getValue());
      WorkflowUtils.createWorkflowRequestActivity(request, login_user
        .getName()
        .getValue(), recipients, Type.REQUEST);

      // 次の申請先にメール送信
      List<ALEipUser> destUsers = new ArrayList<ALEipUser>();
      destUsers.add(nextUser);
      WorkflowUtils.sendMail(
        rundata,
        request,
        destUsers,
        new ArrayList<String>());

      File folder = FileuploadUtils.getFolder(orgId, uid, folderName);
      // 添付ファイル保存先のフォルダを削除
      FileuploadUtils.deleteFolder(folder);

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * データベースに格納されているリクエストを更新します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTWorkflowRequest oldrequest =
        WorkflowUtils.getEipTWorkflowRequestForOwner(rundata, context);
      if (oldrequest == null) {
        return false;
      }

      Date nowDate = Calendar.getInstance().getTime();

      EipTWorkflowCategory category =
        WorkflowUtils.getEipTWorkflowCategory(Long.valueOf(category_id
          .getValue()));

      // 新規オブジェクトモデル
      EipTWorkflowRequest request = Database.create(EipTWorkflowRequest.class);

      // リクエスト名
      request.setRequestName(request_name.getValue());
      // 親ID
      if (oldrequest.getParentId().intValue() == 0) {
        request.setParentId(oldrequest.getRequestId());
      } else {
        request.setParentId(oldrequest.getParentId());
      }
      // カテゴリID
      request.setEipTWorkflowCategory(category);
      // 申請経路ID
      if (is_saved_route) {
        EipTWorkflowRoute route =
          WorkflowUtils.getEipTWorkflowRoute(Long.valueOf(route_id.getValue()));
        request.setEipTWorkflowRoute(route);
      }
      // ユーザーID
      request.setUserId(Integer
        .valueOf((int) login_user.getUserId().getValue()));
      // 優先度
      request.setPriority(Short.valueOf((short) priority.getValue()));
      // メモ
      request.setNote(note.getValue());
      // 金額
      request.setPrice(Long.valueOf(price.getValue()));
      // 承認フラグ
      request.setProgress(WorkflowUtils.DB_PROGRESS_WAIT);
      // 作成日
      request.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      request.setUpdateDate(Calendar.getInstance().getTime());

      // 申請先の設定
      insertEipTWorkflowRequestMap(
        request,
        login_user,
        WorkflowUtils.DB_STATUS_REQUEST,
        0,
        nowDate);

      // 申請先が削除・無効化されていたら次へまわす
      int size = memberList.size();
      ALEipUser toUser;
      int i;
      for (i = 0; i < size; i++) {
        toUser = memberList.get(i);
        if (WorkflowUtils.getUserIsDisabledOrDeleted(toUser
          .getUserId()
          .getValueAsString())) {
          insertEipTWorkflowRequestMap(
            request,
            toUser,
            WorkflowUtils.DB_STATUS_THROUGH,
            i + 1,
            nowDate);
        } else {
          // 0から数えてi番目のユーザーが確認中
          insertEipTWorkflowRequestMap(
            request,
            toUser,
            WorkflowUtils.DB_STATUS_CONFIRM,
            (i + 1),
            nowDate);
          break;
        }
      }

      // i + 1番目のユーザー以降の状態を「確認前」状態にする
      for (int j = i + 1; j < size; j++) {
        insertEipTWorkflowRequestMap(
          request,
          memberList.get(j),
          WorkflowUtils.DB_STATUS_WAIT,
          (j + 1),
          nowDate);
      }

      // 添付ファイル処理
      if (!WorkflowUtils.insertFileDataDelegate(
        rundata,
        context,
        request,
        oldrequest,
        fileuploadList,
        folderName,
        msgList)) {
        return false;
      }

      // リクエストを登録
      Database.commit();

      // 再申請済みを設定する
      oldrequest.setProgress(WorkflowUtils.DB_PROGRESS_REAPPLY);
      // リクエストを登録
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        request.getRequestId(),
        ALEventlogConstants.PORTLET_TYPE_WORKFLOW,
        request.getEipTWorkflowCategory().getCategoryName()
          + " "
          + request_name.getValue());

      // 削除・無効化されていない最初の申請先に新着ポートレット登録
      ALEipUser nextUser = memberList.get(i);

      ALAccessControlFactoryService aclservice =
        (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
          .getInstance())
          .getService(ALAccessControlFactoryService.SERVICE_NAME);
      ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

      if (aclhandler.hasAuthority(
        (int) nextUser.getUserId().getValue(),
        ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF,
        ALAccessControlConstants.VALUE_ACL_DETAIL)) {
        WhatsNewUtils.insertWhatsNew(
          WhatsNewUtils.WHATS_NEW_TYPE_WORKFLOW_REQUEST,
          request.getRequestId().intValue(),
          (int) nextUser.getUserId().getValue());
      }

      // アクティビティ
      List<String> recipients = new ArrayList<String>();
      recipients.add(nextUser.getName().getValue());
      WorkflowUtils.createWorkflowRequestActivity(request, login_user
        .getName()
        .getValue(), recipients, Type.REQUEST);

      // 次の申請先にメール送信
      List<ALEipUser> destUsers = new ArrayList<ALEipUser>();
      destUsers.add(nextUser);
      WorkflowUtils.sendMail(
        rundata,
        request,
        destUsers,
        new ArrayList<String>());

      File folder = FileuploadUtils.getFolder(orgId, uid, folderName);
      // 添付ファイル保存先のフォルダを削除
      FileuploadUtils.deleteFolder(folder);

    } catch (Exception ex) {
      Database.rollback();
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * 
   * @param request
   * @param user
   * @param status
   *          R: 申請 C : 確認 A: 承認 D: 否認
   */
  private void insertEipTWorkflowRequestMap(EipTWorkflowRequest request,
      ALEipUser user, String status, int order, Date now) {
    EipTWorkflowRequestMap map = Database.create(EipTWorkflowRequestMap.class);
    int userid = (int) user.getUserId().getValue();
    map.setEipTWorkflowRequest(request);
    map.setUserId(Integer.valueOf(userid));
    // R: 申請 C : 確認 A: 承認 D: 否認
    map.setStatus(status);
    map.setOrderIndex(Integer.valueOf(order));
    map.setCreateDate(now);
    map.setUpdateDate(now);
  }

  /**
   * テンプレートを渡す
   * 
   * @param num
   * @return
   */
  public String getRouteMap(int num) {
    for (Object o : routeList) {
      WorkflowRouteResultData tmp = (WorkflowRouteResultData) o;
      if (tmp.getRouteId().getValue() == num) {

        String route = tmp.getRoute().getValue();
        String[] routeArray = route.split(",");
        int routeArrayLength = routeArray.length;

        // ユーザー一覧を得る
        List<ALEipUser> list = new ArrayList<ALEipUser>();
        SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
        Expression exp11 =
          ExpressionFactory.matchExp(TurbineUser.DISABLED_PROPERTY, "F");
        Expression exp =
          ExpressionFactory.matchDbExp(
            TurbineUser.USER_ID_PK_COLUMN,
            routeArray[0]);

        Expression exptmp;
        for (int i = 1; i < routeArrayLength; i++) {
          exptmp =
            ExpressionFactory.matchDbExp(
              TurbineUser.USER_ID_PK_COLUMN,
              routeArray[i]);
          exp = exp.orExp(exptmp);
        }
        exp = exp.andExp(exp11);

        query.setQualifier(exp);
        list = ALEipUtils.getUsersFromSelectQuery(query);
        StringBuffer result = new StringBuffer();

        ALEipUser user = null;
        int listsize = list.size();
        for (int i = 0; i < routeArrayLength; i++) {
          for (int j = 0; j < listsize; j++) {
            user = list.get(j);
            String userid = user.getUserId().toString();
            if (userid.equals(routeArray[i])) {
              result.append(user.getName()).append(",").append(
                user.getAliasName().toString()).append(",");
            }
          }
        }
        return result.toString();
      }
    }

    return null;
  }

  /**
   * テンプレートを渡す
   * 
   * @param num
   * @return
   */
  public String getRouteHTemplate(int num) {
    for (Object o : routeList) {
      WorkflowRouteResultData tmp = (WorkflowRouteResultData) o;
      if (tmp.getRouteId().getValue() == num) {
        return tmp.getRouteH();
      }
    }

    return "";
  }

  public String getRouteH() {
    StringBuffer routeun = new StringBuffer();
    String username;

    if (route.getValue() != null && !"".equals(route.getValue())) {
      StringTokenizer st = new StringTokenizer(route.getValue(), ",");
      while (st.hasMoreTokens()) {
        username = WorkflowUtils.getName(st.nextToken());
        routeun.append(username);
        routeun.append(" -> ");
      }
      routeun.append("完了");

      return routeun.toString();
    }
    return "";
  }

  /**
   * カテゴリIDを取得します。 <BR>
   * 
   * @return
   */
  public ALNumberField getCategoryId() {
    return category_id;
  }

  /**
   * 申請経路IDを取得します。 <BR>
   * 
   * @return
   */
  public ALNumberField getRouteId() {
    return route_id;
  }

  /**
   * メモを取得します。 <BR>
   * 
   * @return
   */
  public ALStringField getNote() {
    return note;
  }

  /**
   * メモのフィールドを設定します。 <BR>
   * 
   * @param str
   * @return
   */
  public void setNote(String str) {
    note.setValue(str);
  }

  /**
   * 金額を取得します。 <BR>
   * 
   * @return
   */
  public ALNumberField getPrice() {
    return price;
  }

  /**
   * 金額を設定します。 <BR>
   * 
   * @param str
   * @return
   */
  public void setPrice(String str) {
    price.setValue(str);
  }

  /**
   * 優先度を取得します。 <BR>
   * 
   * @return
   */
  public ALNumberField getPriority() {
    return priority;
  }

  /**
   * 優先度を設定します。 <BR>
   * 
   * @param str
   * @return
   */
  public void setPriority(String str) {
    priority.setValue(str);
  }

  /**
   * リクエスト名を取得します。 <BR>
   * 
   * @return
   */
  public ALStringField getRequestName() {
    return request_name;
  }

  /**
   * リクエスト名を格納します。 <BR>
   * 
   * @param str
   * @return
   */

  public void setRequestName(String str) {
    request_name.setValue(str);
  }

  /**
   * カテゴリ一覧を取得します。 <BR>
   * 
   * @return
   */
  public List<WorkflowCategoryResultData> getCategoryList() {
    return categoryList;
  }

  /**
   * 申請経路一覧を取得します。 <BR>
   * 
   * @return
   */
  public List<WorkflowRouteResultData> getRouteList() {
    return routeList;
  }

  /**
   * グループメンバーを取得します。 <BR>
   * 
   * @return
   */
  public List<ALEipUser> getMemberList() {
    return memberList;
  }

  /**
   * グループメンバーを格納します。 <BR>
   * 
   * @param str
   * @return
   */
  public void setMemberList(ArrayList<ALEipUser> list) {
    memberList = list;
  }

  /**
   * 
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
  }

  /**
   * 
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  /**
   * @return
   */
  public boolean getIsSavedRoute() {
    return is_saved_route;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_WORKFLOW_REQUEST_SELF;
  }
}
