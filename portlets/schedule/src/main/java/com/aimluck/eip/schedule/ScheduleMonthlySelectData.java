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

package com.aimluck.eip.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.FacilityResultData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.todo.util.ToDoUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 月間スケジュールの検索結果を管理するクラスです。
 * 
 */
public class ScheduleMonthlySelectData extends
    ALAbstractSelectData<EipTScheduleMap, EipTScheduleMap> {
  /** <code>TARGET_GROUP_NAME</code> グループによる表示切り替え用変数の識別子 */
  private final String TARGET_GROUP_NAME = "target_group_name";

  /** <code>TARGET_USER_ID</code> ユーザによる表示切り替え用変数の識別子 */
  private final String TARGET_USER_ID = "target_user_id";

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger =
    JetspeedLogFactoryService.getLogger(ScheduleMonthlySelectData.class
      .getName());

  /** <code>viewMonth</code> 現在の月 */
  private ALDateTimeField viewMonth;

  /** <code>prevMonth</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextMonth</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>currentMonth</code> 今月 */
  private ALDateTimeField currentMonth;

  /** <code>today</code> 今日 */
  private ALDateTimeField today;

  /** <code>viewStart</code> 表示開始日時 */
  private ALDateTimeField viewStart;

  /** <code>viewEnd</code> 表示終了日時 */
  private ALDateTimeField viewEnd;

  /** <code>viewEndCrt</code> 表示終了日時 (Criteria) */
  private ALDateTimeField viewEndCrt;

  /** <code>viewtype</code> 表示タイプ */
  private String viewtype;

  /** <code>monthCon</code> 月間スケジュールコンテナ */
  private ScheduleMonthContainer monthCon;

  /** <code>target_group_name</code> 表示対象の部署名 */
  private String target_group_name;

  /** <code>target_user_id</code> 表示対象のユーザ ID */
  private String target_user_id;

  /** <code>myGroupList</code> グループリスト（My グループと部署） */
  private List<ALEipGroup> myGroupList = null;

  /** <code>userList</code> 表示切り替え用のユーザリスト */
  private List<ALEipUser> userList = null;

  /** <code>userid</code> ユーザーID */
  private String userid;

  /** <code>monthTodoCon</code> 期間スケジュール用の月間コンテナ */
  private ScheduleTermMonthContainer termMonthCon;

  /** <code>monthTodoCon</code> 月間 ToDo コンテナ */
  private ScheduleToDoMonthContainer monthTodoCon;

  /** <code>viewTodo</code> ToDo 表示設定 */
  protected int viewTodo;

  /** ポートレット ID */
  private String portletId;

  /** <code>facilityList</code> 表示切り替え用の施設リスト */
  private List<FacilityResultData> facilityList;

  /** 閲覧権限の有無 */
  private boolean hasAclviewOther;

  /** <code>hasAuthoritySelfInsert</code> アクセス権限 */
  private boolean hasAuthoritySelfInsert = false;

  /** <code>hasAuthorityFacilityInsert</code> アクセス権限 */
  private boolean hasAuthorityFacilityInsert = false;

  /** <code>target_user_id</code> 表示対象のユーザ ログイン名 */
  private String target_user_name;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // 展開されるパラメータは以下の通りです。
    // ・viewMonth 形式：yyyy-MM

    // 表示種別の設定
    viewtype = "monthly";
    // POST/GET から yyyy-MM の形式で受け渡される。
    // 現在の月
    viewMonth = new ALDateTimeField("yyyy-MM");
    viewMonth.setNotNull(true);
    // 前の月
    prevMonth = new ALDateTimeField("yyyy-MM");
    // 次の月
    nextMonth = new ALDateTimeField("yyyy-MM");
    // 今月
    currentMonth = new ALDateTimeField("yyyy-MM");
    // 表示開始日時
    viewStart = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時
    viewEnd = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時 (Criteria)
    viewEndCrt = new ALDateTimeField("yyyy-MM-dd");
    // 今日
    today = new ALDateTimeField("yyyy-MM-dd");
    Calendar to = Calendar.getInstance();
    to.set(Calendar.HOUR_OF_DAY, 0);
    to.set(Calendar.MINUTE, 0);
    today.setValue(to.getTime());
    currentMonth.setValue(to.getTime());

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // スケジュールの表示開始日時
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("view_month")) {
        ALEipUtils.setTemp(rundata, context, "view_month", rundata
          .getParameters()
          .getString("view_month"));
      }
    }

    // 現在の月
    String tmpViewMonth = ALEipUtils.getTemp(rundata, context, "view_month");
    if (tmpViewMonth == null || tmpViewMonth.equals("")) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.DATE, 1);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewMonth.setValue(cal.getTime());
    } else {
      viewMonth.setValue(tmpViewMonth);
      if (!viewMonth.validate(new ArrayList<String>())) {
        ALEipUtils.removeTemp(rundata, context, "view_month");
        throw new ALPageNotFoundException();
      }
    }

    // 表示開始日時
    Calendar cal = Calendar.getInstance();
    Calendar tmpCal = Calendar.getInstance();
    cal.setTime(viewMonth.getValue());
    tmpCal.setTime(viewMonth.getValue());
    int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
    cal.add(Calendar.DATE, -dayofweek + 1);
    viewStart.setValue(cal.getTime());

    Calendar cal4 = Calendar.getInstance();
    cal4.setTime(cal.getTime());
    Calendar tmpCal4 = Calendar.getInstance();
    tmpCal4.setTime(tmpCal.getTime());

    Calendar cal5 = Calendar.getInstance();
    cal5.setTime(cal.getTime());
    Calendar tmpCal5 = Calendar.getInstance();
    tmpCal5.setTime(tmpCal.getTime());

    // 月間スケジュールコンテナの初期化
    try {
      termMonthCon = new ScheduleTermMonthContainer();
      termMonthCon.initField();
      termMonthCon.setViewMonth(cal4, tmpCal4);

      monthCon = new ScheduleMonthContainer();
      monthCon.initField();
      monthCon.setViewMonth(cal, tmpCal);

      monthTodoCon = new ScheduleToDoMonthContainer();
      monthTodoCon.initField();
      monthTodoCon.setViewMonth(cal5, tmpCal5);
    } catch (Exception e) {
      logger.error("Exception", e);
    }
    // 表示終了日時
    viewEndCrt.setValue(cal.getTime());
    cal.add(Calendar.DATE, -1);
    viewEnd.setValue(cal.getTime());
    // 次の月、前の月
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewMonth.getValue());
    cal2.add(Calendar.MONTH, 1);
    nextMonth.setValue(cal2.getTime());
    cal2.add(Calendar.MONTH, -2);
    prevMonth.setValue(cal2.getTime());

    ALEipUtils.setTemp(rundata, context, "tmpStart", viewStart.toString()
      + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", viewStart.toString()
      + "-00-00");

    // ログインユーザの ID を設定する．
    userid = Integer.toString(ALEipUtils.getUserId(rundata));

    // My グループの一覧を取得する．
    List<ALEipGroup> myGroups = ALEipUtils.getMyGroups(rundata);
    myGroupList = new ArrayList<ALEipGroup>();
    int length = myGroups.size();
    for (int i = 0; i < length; i++) {
      myGroupList.add(myGroups.get(i));
    }

    try {
      String groupFilter =
        ALEipUtils.getTemp(rundata, context, TARGET_GROUP_NAME);
      if (groupFilter == null || groupFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        groupFilter = portlet.getPortletConfig().getInitParameter("p3a-group");
        if (groupFilter != null) {
          ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, groupFilter);
        }
      }

      // スケジュールを表示するユーザ ID をセッションに設定する．
      String userFilter = ALEipUtils.getTemp(rundata, context, TARGET_USER_ID);
      if (userFilter == null || userFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        userFilter = portlet.getPortletConfig().getInitParameter("p3a-user");
      }

      if (userFilter != null && (!userFilter.equals(""))) {
        int paramId = -1;
        if (userFilter.startsWith(ScheduleUtils.TARGET_FACILITY_ID)) {
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, userFilter);
        } else {
          try {
            paramId = Integer.parseInt(userFilter);
            if (paramId > 3) {
              // ユーザーIDを取得する
              String query =
                "SELECT LOGIN_NAME FROM turbine_user WHERE USER_ID = '"
                  + paramId
                  + "' AND DISABLED = 'F'";
              List<TurbineUser> list =
                Database.sql(TurbineUser.class, query).fetchList();
              if (list != null && list.size() != 0) {
                // 指定したユーザが存在する場合，セッションに保存する．
                ALEipUtils
                  .setTemp(rundata, context, TARGET_USER_ID, userFilter);
              } else {
                ALEipUtils.removeTemp(rundata, context, TARGET_USER_ID);
              }
            }
          } catch (NumberFormatException e) {
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    // ToDo 表示設定
    viewTodo =
      Integer.parseInt(ALEipUtils
        .getPortlet(rundata, context)
        .getPortletConfig()
        .getInitParameter("p5a-view"));

    // アクセスコントロール
    int loginUserId = ALEipUtils.getUserId(rundata);

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    hasAclviewOther =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_OTHER,
        ALAccessControlConstants.VALUE_ACL_DETAIL);

    hasAuthoritySelfInsert =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_INSERT);

    hasAuthorityFacilityInsert =
      aclhandler.hasAuthority(
        loginUserId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_FACILITY,
        ALAccessControlConstants.VALUE_ACL_INSERT);

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
  }

  /**
   * 
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected ResultList<EipTScheduleMap> selectList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // 指定グループや指定ユーザをセッションに設定する．
      setupLists(rundata, context);

      SelectQuery<EipTScheduleMap> query = getSelectQuery(rundata, context);
      if (query == null) {
        return null;
      }
      List<EipTScheduleMap> list = query.fetchList();

      if (!target_user_id.startsWith(ScheduleUtils.TARGET_FACILITY_ID)
        && viewTodo == 1) {
        // ToDo の読み込み
        loadTodo(rundata, context);
      }

      // 時刻でソート
      ScheduleUtils.sortByTime(list);

      return new ResultList<EipTScheduleMap>(ScheduleUtils
        .sortByDummySchedule(list));
    } catch (Exception e) {
      logger.error("[ScheduleMonthlySelectData]", e);
      throw new ALDBErrorException();
    }

  }

  /**
   * 検索条件を設定した SelectQuery を返します。
   * 
   * @param rundata
   * @param context
   * @return
   */
  private SelectQuery<EipTScheduleMap> getSelectQuery(RunData rundata,
      Context context) {
    SelectQuery<EipTScheduleMap> query = Database.query(EipTScheduleMap.class);

    // 終了日時
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(
        EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
          + "."
          + EipTSchedule.END_DATE_PROPERTY,
        viewStart.getValue());
    // 開始日時
    Expression exp12 =
      ExpressionFactory.lessOrEqualExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
        + "."
        + EipTSchedule.START_DATE_PROPERTY, viewEndCrt.getValue());
    // 通常スケジュール
    Expression exp13 =
      ExpressionFactory.noMatchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
        + "."
        + EipTSchedule.REPEAT_PATTERN_PROPERTY, "N");
    // 期間スケジュール
    Expression exp14 =
      ExpressionFactory.noMatchExp(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY
        + "."
        + EipTSchedule.REPEAT_PATTERN_PROPERTY, "S");

    query.setQualifier((exp11.andExp(exp12)).orExp(exp13.andExp(exp14)));

    if ((target_user_id != null) && (!target_user_id.equals(""))) {
      if (target_user_id.startsWith(ScheduleUtils.TARGET_FACILITY_ID)) {
        String fid =
          target_user_id.substring(
            ScheduleUtils.TARGET_FACILITY_ID.length(),
            target_user_id.length());
        // 指定ユーザをセットする．
        Expression exp1 =
          ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, fid);
        query.andQualifier(exp1);
        // 設備のスケジュール
        Expression exp2 =
          ExpressionFactory.matchExp(
            EipTScheduleMap.TYPE_PROPERTY,
            ScheduleUtils.SCHEDULEMAP_TYPE_FACILITY);
        query.andQualifier(exp2);
      } else {
        // 指定ユーザをセットする．
        Expression exp3 =
          ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
            .valueOf(target_user_id));
        query.andQualifier(exp3);
        // ユーザのスケジュール
        Expression exp4 =
          ExpressionFactory.matchExp(
            EipTScheduleMap.TYPE_PROPERTY,
            ScheduleUtils.SCHEDULEMAP_TYPE_USER);
        query.andQualifier(exp4);
      }
    } else {
      // 表示できるユーザがいない場合の処理
      return null;
    }

    return query;
  }

  /**
   * 
   * @param record
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultData(EipTScheduleMap record)
      throws ALPageNotFoundException, ALDBErrorException {
    ScheduleResultData rd = new ScheduleResultData();
    rd.initField();
    try {
      EipTSchedule schedule = record.getEipTSchedule();
      // スケジュールが棄却されている場合は表示しない
      if ("R".equals(record.getStatus())) {
        return rd;
      }
      int userid_int = Integer.parseInt(userid);

      SelectQuery<EipTScheduleMap> mapquery =
        Database.query(EipTScheduleMap.class);
      Expression mapexp1 =
        ExpressionFactory.matchExp(
          EipTScheduleMap.SCHEDULE_ID_PROPERTY,
          schedule.getScheduleId());
      mapquery.setQualifier(mapexp1);
      Expression mapexp2 =
        ExpressionFactory.matchExp(EipTScheduleMap.USER_ID_PROPERTY, Integer
          .valueOf(userid));
      mapquery.andQualifier(mapexp2);

      List<EipTScheduleMap> schedulemaps = mapquery.fetchList();
      boolean is_member =
        (schedulemaps != null && schedulemaps.size() > 0) ? true : false;

      // Dummy スケジュールではない
      // 完全に隠す
      // 自ユーザー以外
      // 共有メンバーではない
      // オーナーではない
      if ((!"D".equals(record.getStatus()))
        && "P".equals(schedule.getPublicFlag())
        && (userid_int != record.getUserId().intValue())
        && (userid_int != schedule.getOwnerId().intValue())
        && !is_member) {
        return rd;
      }

      if ("C".equals(schedule.getPublicFlag())
        && (userid_int != record.getUserId().intValue())
        && (userid_int != schedule.getOwnerId().intValue())
        && !is_member) {
        // 名前
        rd.setName("非公開");
        // 仮スケジュールかどうか
        rd.setTmpreserve(false);
      } else {
        // 名前
        rd.setName(schedule.getName());
        // 仮スケジュールかどうか
        rd.setTmpreserve("T".equals(record.getStatus()));
      }
      // 場所
      rd.setPlace(schedule.getPlace());
      // ID
      rd.setScheduleId(schedule.getScheduleId().intValue());
      // 親スケジュール ID
      rd.setParentId(schedule.getParentId().intValue());
      // 開始日時
      rd.setStartDate(schedule.getStartDate());
      // 終了日時
      rd.setEndDate(schedule.getEndDate());
      // 公開するかどうか
      rd.setPublic("O".equals(schedule.getPublicFlag()));
      // 非表示にするかどうか
      rd.setHidden("P".equals(schedule.getPublicFlag()));
      // ダミーか
      rd.setDummy("D".equals(record.getStatus()));
      // ログインユーザかどうか
      rd.setLoginuser(record.getUserId().intValue() == userid_int);
      // オーナーかどうか
      rd.setOwner(schedule.getOwnerId().intValue() == userid_int);
      // 共有メンバーかどうか
      rd.setMember(is_member);
      // 繰り返しパターン
      rd.setPattern(schedule.getRepeatPattern());

      // 期間スケジュールの場合
      if (rd.getPattern().equals("S")) {
        int stime =
          -(int) ((viewStart.getValue().getTime() - rd
            .getStartDate()
            .getValue()
            .getTime()) / 86400000);
        int etime =
          -(int) ((viewStart.getValue().getTime() - rd
            .getEndDate()
            .getValue()
            .getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        int row = count / 7;
        count = count % 7;
        // 行をまたがる場合
        while (count + col > 7) {
          ScheduleResultData rd3 = (ScheduleResultData) rd.clone();
          rd3.setRowspan(7 - count);
          // monthCon.addSpanResultData(count, row, rd3);
          termMonthCon.addTermResultData(count, row, rd3);
          count = 0;
          col -= rd3.getRowspan();
          row++;
        }
        // rowspanを設定
        rd.setRowspan(col);
        if (col > 0) {
          // 期間スケジュールをコンテナに格納
          termMonthCon.addTermResultData(count, row, rd);
        } else {

        }
        return rd;
      }

      // スケジュールをコンテナに格納
      monthCon.addResultData(rd);
    } catch (Exception e) {
      logger.error("Exception", e);

      return null;
    }
    return rd;
  }

  /**
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected EipTScheduleMap selectDetail(RunData rundata, Context context) {
    return null;
  }

  /**
   * 
   * @param record
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTScheduleMap record) {
    return null;
  }

  /*
   *
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  public void loadTodo(RunData rundata, Context context) {
    try {
      SelectQuery<EipTTodo> query = getSelectQueryForTodo(rundata, context);
      List<EipTTodo> todos = query.fetchList();

      int todossize = todos.size();
      for (int i = 0; i < todossize; i++) {
        EipTTodo record = todos.get(i);
        ScheduleToDoResultData rd = new ScheduleToDoResultData();
        rd.initField();

        // ポートレット ToDo のへのリンクを取得する．
        String todo_url = "";
        if (userid.equals(target_user_id)) {
          todo_url =
            ScheduleUtils.getPortletURItoTodoDetailPane(rundata, "ToDo", record
              .getTodoId()
              .longValue(), portletId);
        } else {
          todo_url =
            ScheduleUtils.getPortletURItoTodoPublicDetailPane(
              rundata,
              "ToDo",
              record.getTodoId().longValue(),
              portletId);
        }
        rd.setTodoId(record.getTodoId().longValue());
        rd.setTodoName(record.getTodoName());
        rd.setUserId(record.getTurbineUser().getUpdatedUserId().intValue());
        rd.setStartDate(record.getStartDate());
        rd.setEndDate(record.getEndDate());
        rd.setTodoUrl(todo_url);
        // 公開/非公開を設定する．
        rd.setPublicFlag("T".equals(record.getPublicFlag()));

        int stime;
        if (ScheduleUtils.equalsToDate(ToDoUtils.getEmptyDate(), rd
          .getStartDate()
          .getValue(), false)) {
          stime = 0;
        } else {
          stime =
            -(int) ((viewStart.getValue().getTime() - rd
              .getStartDate()
              .getValue()
              .getTime()) / 86400000);
        }
        int etime =
          -(int) ((viewStart.getValue().getTime() - rd
            .getEndDate()
            .getValue()
            .getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        int row = count / 7;
        count = count % 7;
        // 行をまたがる場合
        while (count + col > 7) {
          ScheduleToDoResultData rd3 = (ScheduleToDoResultData) rd.clone();
          rd3.setRowspan(7 - count);
          monthTodoCon.addToDoResultData(count, row, rd3);
          count = 0;
          col -= rd3.getRowspan();
          row++;
        }
        // rowspanを設定
        rd.setRowspan(col);
        if (col > 0) {
          // 期間スケジュールをコンテナに格納
          monthTodoCon.addToDoResultData(count, row, rd);
        }
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return;
    }
  }

  private SelectQuery<EipTTodo> getSelectQueryForTodo(RunData rundata,
      Context context) {
    SelectQuery<EipTTodo> query = Database.query(EipTTodo.class);
    Expression exp1 =
      ExpressionFactory.noMatchExp(EipTTodo.STATE_PROPERTY, Short
        .valueOf((short) 100));
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.matchExp(EipTTodo.ADDON_SCHEDULE_FLG_PROPERTY, "T");
    query.andQualifier(exp2);

    if ((target_user_id != null) && (!target_user_id.equals(""))) {
      // 指定ユーザをセットする．
      Expression exp3 =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(target_user_id));
      query.andQualifier(exp3);
    } else {
      // 表示できるユーザがいない場合の処理
      return null;
    }

    if (!userid.equals(target_user_id)) {
      Expression exp4 =
        ExpressionFactory.matchExp(EipTTodo.PUBLIC_FLAG_PROPERTY, "T");
      query.andQualifier(exp4);
    }

    // 終了日時
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY,
        getViewStart().getValue());
    // 開始日時
    Expression exp12 =
      ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY,
        getViewEnd().getValue());

    // 開始日時のみ指定されている ToDo を検索
    Expression exp21 =
      ExpressionFactory.lessOrEqualExp(
        EipTTodo.START_DATE_PROPERTY,
        getViewEnd().getValue());
    Expression exp22 =
      ExpressionFactory.matchExp(EipTTodo.END_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    // 終了日時のみ指定されている ToDo を検索
    Expression exp31 =
      ExpressionFactory.greaterOrEqualExp(
        EipTTodo.END_DATE_PROPERTY,
        getViewStart().getValue());
    Expression exp32 =
      ExpressionFactory.matchExp(EipTTodo.START_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)).orExp(
      exp31.andExp(exp32)));

    query.orderAscending(EipTTodo.START_DATE_PROPERTY);
    return query;
  }

  /**
   * 表示タイプを取得します。
   * 
   * @return
   */
  public String getViewtype() {
    return viewtype;
  }

  /**
   * 表示開始日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewStart() {
    return viewStart;
  }

  /**
   * 表示終了日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEnd() {
    return viewEnd;
  }

  /**
   * 表示終了日時 (Criteria) を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEndCrt() {
    return viewEndCrt;
  }

  /**
   * 前の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevMonth() {
    return prevMonth;
  }

  /**
   * 次の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextMonth() {
    return nextMonth;
  }

  /**
   * 現在の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewMonth() {
    return viewMonth;
  }

  /**
   * 今日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getToday() {
    return today;
  }

  /**
   * 今月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getCurrentMonth() {
    return currentMonth;
  }

  /**
   * 月間スケジュールコンテナを取得します。
   * 
   * @return
   */
  public ScheduleMonthContainer getContainer() {
    return monthCon;
  }

  /**
   * 指定グループや指定ユーザをセッションに設定する．
   * 
   * @param rundata
   * @param context
   * @throws ALDBErrorException
   */
  private void setupLists(RunData rundata, Context context) {
    target_group_name = getTargetGroupName(rundata, context);
    if ((target_group_name != null)
      && (!target_group_name.equals(""))
      && (!target_group_name.equals("all"))) {
      userList = ALEipUtils.getUsers(target_group_name);
      facilityList = FacilitiesUtils.getFacilityList(target_group_name);
    } else {
      userList = ALEipUtils.getUsers("LoginUser");
      facilityList =
        FacilitiesUtils
          .getFacilitiesFromSelectQuery(new com.aimluck.eip.orm.query.SelectQuery<EipMFacility>(
            EipMFacility.class));
    }

    if ((userList == null || userList.size() == 0)
      && (facilityList == null || facilityList.size() == 0)) {
      target_user_id = "";
      ALEipUtils.removeTemp(rundata, context, TARGET_USER_ID);
      return;
    }

    target_user_id = getTargetUserId(rundata, context);
    try {
      if ("".equals(target_user_id) || target_user_id.startsWith("f")) {
        target_user_name = null;
      } else {
        ALEipUser tempuser =
          ALEipUtils.getALEipUser(Integer.parseInt(target_user_id));
        target_user_name = tempuser.getName().getValue();
      }
    } catch (Exception e) {
      logger.error(e);
      target_user_name = null;
    }
  }

  /**
   * 表示切り替えで指定したグループ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  private String getTargetGroupName(RunData rundata, Context context) {
    String target_group_name = null;
    String idParam = null;
    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，グループ名を取得する．
      idParam = rundata.getParameters().getString(TARGET_GROUP_NAME);
    }
    target_group_name = ALEipUtils.getTemp(rundata, context, TARGET_GROUP_NAME);

    if (idParam == null && target_group_name == null) {
      ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, "all");
      target_group_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, idParam);
      target_group_name = idParam;
    }
    return target_group_name;
  }

  /**
   * 表示切り替えで指定したユーザ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  private String getTargetUserId(RunData rundata, Context context) {
    String target_user_id = null;
    String idParam = null;
    String tmp_user_id = "";

    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，ユーザ ID を取得する．
      idParam = rundata.getParameters().getString(TARGET_USER_ID);
    }
    target_user_id = ALEipUtils.getTemp(rundata, context, TARGET_USER_ID);

    if ("Facility".equals(getTargetGroupName())) {
      // 表示グループで「施設一覧」が選択されている場合
      if (facilityList != null && facilityList.size() > 0) {
        if (idParam == null && (target_user_id == null)) {
          tmp_user_id = "";
        } else if (idParam != null) {
          tmp_user_id = idParam;
        } else {
          tmp_user_id = target_user_id;
        }

        if (containsFacilityId(facilityList, tmp_user_id)) {
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, tmp_user_id);
          target_user_id = tmp_user_id;
        } else {
          FacilityResultData rd = facilityList.get(0);
          target_user_id = "f" + rd.getFacilityId().getValue();
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, target_user_id);
        }
      }
    } else {
      if (idParam == null && (target_user_id == null)) {
        tmp_user_id = userid;
      } else if (idParam != null) {
        tmp_user_id = idParam;
      } else {
        tmp_user_id = target_user_id;
      }

      if (tmp_user_id.startsWith("f")) {
        if (containsFacilityId(facilityList, tmp_user_id)) {
          ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, tmp_user_id);
          target_user_id = tmp_user_id;
        } else {
          if (facilityList != null && facilityList.size() > 0) {
            FacilityResultData rd = facilityList.get(0);
            target_user_id = "f" + rd.getFacilityId().getValue();
            ALEipUtils
              .setTemp(rundata, context, TARGET_USER_ID, target_user_id);
          } else {
            target_user_id = userid;
          }
        }
      } else {
        if (userList != null && userList.size() > 0) {
          // グループで表示を切り替えた場合，
          // ログインユーザもしくはユーザリストの一番初めのユーザを
          // 表示するため，ユーザ ID を設定する．
          if (containsUserId(userList, tmp_user_id)) {
            ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, tmp_user_id);
            target_user_id = tmp_user_id;
          } else if (containsUserId(userList, userid)) {
            // ログインユーザのスケジュールを表示するため，ログイン ID を設定する．
            ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, userid);
            target_user_id = userid;
          } else {
            ALEipUser eipUser = userList.get(0);
            String userId = eipUser.getUserId().getValueAsString();
            ALEipUtils.setTemp(rundata, context, TARGET_USER_ID, userId);
            target_user_id = userId;
          }
        }
      }
    }

    return target_user_id;
  }

  private boolean containsUserId(List<ALEipUser> list, String userid) {
    if (list == null || list.size() <= 0) {
      return false;
    }

    ALEipUser eipUser;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      eipUser = list.get(i);
      String eipUserId = eipUser.getUserId().getValueAsString();
      if (userid.equals(eipUserId)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsFacilityId(List<FacilityResultData> list,
      String facility_id) {
    if (list == null || list.size() <= 0) {
      return false;
    }

    FacilityResultData facility;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      facility = list.get(i);
      String fid = "f" + facility.getFacilityId().toString();
      if (facility_id.equals(fid)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 表示切り替え時に指定するグループ名
   * 
   * @return
   */
  public String getTargetGroupName() {
    return target_group_name;
  }

  /**
   * 表示切り替え時に指定するユーザ ID
   * 
   * @return
   */
  public String getTargetUserId() {
    return target_user_id;
  }

  /**
   * 指定グループに属するユーザの一覧を取得する．
   * 
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers() {
    if (hasAclviewOther) {
      return userList;
    } else {
      try {
        List<ALEipUser> users = new ArrayList<ALEipUser>();
        users.add(ALEipUtils.getALEipUser(Integer.parseInt(userid)));
        return users;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * 部署の一覧を取得する．
   * 
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    if (hasAclviewOther) {
      return ALEipManager.getInstance().getPostMap();
    } else {
      return null;
    }
  }

  /**
   * My グループの一覧を取得する．
   * 
   * @return
   */
  public List<ALEipGroup> getMyGroupList() {
    if (hasAclviewOther) {
      return myGroupList;
    } else {
      return null;
    }
  }

  /**
   * ログインユーザの ID を取得する．
   * 
   * @return
   */
  public String getUserId() {
    return userid;
  }

  /**
   * 期間スケジュール用の月間コンテナを取得する.
   * 
   * @return
   */
  public ScheduleTermMonthContainer getTermContainer() {
    return termMonthCon;
  }

  /**
   * 月間 ToDo コンテナを取得する.
   * 
   * @return
   */
  public ScheduleToDoMonthContainer getToDoContainer() {
    return monthTodoCon;
  }

  public void setPortletId(String id) {
    portletId = id;
    // userid.substring(1);
  }

  public List<FacilityResultData> getFacilityList() {
    return facilityList;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  public boolean hasAuthoritySelfInsert() {
    return hasAuthoritySelfInsert;
  }

  public boolean hasAuthorityFacilityInsert() {
    return hasAuthorityFacilityInsert;
  }

  /**
   * 表示切り替え時に指定するユーザ のログイン名前
   * 
   * @return
   */
  public String getTargetUserName() {
    return target_user_name;
  }

}
