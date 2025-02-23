/*******************************************************************************
 * Copyright (c) 2011  University of Western Australia. All rights reserved.
 * 
 * This file is part of The Ark.
 * 
 * The Ark is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * The Ark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package au.org.theark.lims.web.component.biospecimenupload;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import au.org.theark.core.exception.EntityNotFoundException;
import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.ArkModule;
import au.org.theark.core.model.study.entity.ArkUser;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.Upload;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.vo.ArkUserVO;
import au.org.theark.core.vo.UploadVO;
import au.org.theark.core.web.component.AbstractContainerPanel;
import au.org.theark.lims.web.component.biospecimenupload.form.ContainerForm;

public class BiospecimenUploadContainerPanel extends AbstractContainerPanel<UploadVO> {

	@SpringBean(name = au.org.theark.core.Constants.ARK_COMMON_SERVICE)
	private IArkCommonService					iArkCommonService;

	private static final long					serialVersionUID	= 1L;

	// Panels
	private SearchPanel							searchComponentPanel;
	private SearchResultListPanel				searchResultPanel;
	private DetailPanel							detailPanel;
	private WizardPanel							wizardPanel;
	private PageableListView<Upload>	listView;
	private ContainerForm						containerForm;
	private ArkFunction arkFunction;

	public BiospecimenUploadContainerPanel(String id, ArkFunction arkFunction) {
		super(id);

		/* Initialise the CPM */
		cpModel = new CompoundPropertyModel<UploadVO>(new UploadVO());
		this.arkFunction = arkFunction;
		/* Bind the CPM to the Form */
		containerForm = new ContainerForm("containerForm", cpModel);
		
		Long sessionStudyId = (Long) SecurityUtils.getSubject().getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
		Study study = null;
		if(sessionStudyId != null) {
			study = iArkCommonService.getStudy(sessionStudyId);
		}
		cpModel.getObject().getUpload().setStudy(study);
		containerForm.add(initialiseFeedBackPanel());
		// containerForm.add(initialiseDetailPanel());
		containerForm.add(initialiseWizardPanel());
		containerForm.add(initialiseSearchResults());
		// containerForm.add(initialiseSearchPanel());
		containerForm.setMultiPart(true);
		add(containerForm);
	}

	private WebMarkupContainer initialiseWizardPanel() {
		wizardPanel = new WizardPanel("wizardPanel", feedBackPanel, containerForm, arkCrudContainerVO);
		wizardPanel.initialisePanel();
		arkCrudContainerVO.getWizardPanelContainer().setVisible(true);
		arkCrudContainerVO.getWizardPanelContainer().add(wizardPanel);
		return arkCrudContainerVO.getWizardPanelContainer();
	}

	protected WebMarkupContainer initialiseSearchResults() {
		searchResultPanel = new SearchResultListPanel("searchResults", feedBackPanel, containerForm, arkCrudContainerVO);
		
		

		iModel = new LoadableDetachableModel<Object>() {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected Object load() {
				// Return all Uploads for the Study in context
				java.util.Collection<Upload> studyUploads = new ArrayList<Upload>();
				if (isActionPermitted()) {
					Upload studyUpload = new Upload();
					List<Study> studyListForUser = new ArrayList<Study>(0);
					Long sessionStudyId = (Long) SecurityUtils.getSubject().getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
					Study study = null;
					if(sessionStudyId != null) {
						study = iArkCommonService.getStudy(sessionStudyId);
						studyUpload.setStudy(study);
						studyUpload.setArkFunction(arkFunction);
						studyListForUser.add(study);
						studyUploads = iArkCommonService.searchUploadsForBiospecimen(studyUpload, studyListForUser);
					}
				}
				listView.removeAll();
				return studyUploads;
			}
		};

		listView = searchResultPanel.buildPageableListView(iModel);
		listView.setReuseItems(true);
		PagingNavigator pageNavigator = new PagingNavigator("navigator", listView);
		searchResultPanel.add(pageNavigator);
		searchResultPanel.add(listView);
		arkCrudContainerVO.getSearchResultPanelContainer().add(searchResultPanel);
		searchResultPanel.setVisible(true);

		return arkCrudContainerVO.getSearchResultPanelContainer();
	}

	protected WebMarkupContainer initialiseDetailPanel() {
		detailPanel = new DetailPanel("detailPanel", feedBackPanel, containerForm, arkCrudContainerVO,arkFunction);
		detailPanel.initialisePanel();
		arkCrudContainerVO.getDetailPanelContainer().add(detailPanel);
		return arkCrudContainerVO.getDetailPanelContainer();
	}

	protected WebMarkupContainer initialiseSearchPanel() {
		searchComponentPanel = new SearchPanel("searchPanel", feedBackPanel, listView, containerForm, arkCrudContainerVO);
		searchComponentPanel.initialisePanel();
		searchComponentPanel.setVisible(false);
		arkCrudContainerVO.getSearchPanelContainer().add(searchComponentPanel);
		return arkCrudContainerVO.getSearchPanelContainer();
	}
}
