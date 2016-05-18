/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2016 TOWN, Inc.
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

package com.aimluck.eip.modules.screens;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.facilities.FacilityGroupChangeTurnFormData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 施設グループの順番情報を処理するクラスです。 <br />
 *
 */
public class FacilityGroupChangeTurnFormScreen extends ALVelocityScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FacilityGroupChangeTurnFormScreen.class.getName());

  /**
   *
   * @param rundata
   * @param context
   * @throws Exception
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {

    try {
      doFacility_form(rundata, context);
    } catch (Exception ex) {
      logger.error("[FacilityGroupChangeTurnFormScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  protected void doFacility_form(RunData rundata, Context context) {
    // ユーザ情報の詳細画面や編集画面からの遷移時に，
    // セッションに残る ENTITY_ID を削除する．
    ALEipUtils.removeTemp(rundata, context, ALEipConstants.ENTITY_ID);

    FacilityGroupChangeTurnFormData formData =
      new FacilityGroupChangeTurnFormData();
    formData.initField();
    formData.doViewForm(this, rundata, context);
    String layout_template =
      "portlets/html/ajax-facility-group-change-turn-form.vm";
    setTemplate(rundata, context, layout_template);
  }

  /**
   * @return
   */
  @Override
  protected String getPortletName() {
    return FacilitiesUtils.FACILITIES_PORTLET_NAME;
  }
}