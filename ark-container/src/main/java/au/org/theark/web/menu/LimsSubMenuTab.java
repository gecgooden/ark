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
package au.org.theark.web.menu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.ArkModule;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.session.ArkSession;
import au.org.theark.core.web.component.customfield.CustomFieldContainerPanel;
import au.org.theark.core.web.component.customfieldcategory.CustomFieldCategoryContainerPanel;
import au.org.theark.core.web.component.customfieldupload.CustomFieldUploadContainerPanel;
import au.org.theark.core.web.component.menu.AbstractArkTabPanel;
import au.org.theark.core.web.component.tabbedPanel.ArkAjaxTabbedPanel;
import au.org.theark.lims.service.IInventoryService;
import au.org.theark.lims.web.Constants;
import au.org.theark.lims.web.component.barcodelabel.BarcodeLabelContainerPanel;
import au.org.theark.lims.web.component.biocollectioncustomdata.BioCollectionCustomDataContainerPanel;
import au.org.theark.lims.web.component.biospecimencustomdata.BiospecimenCustomDataContainerPanel;
import au.org.theark.lims.web.component.biospecimenuidtemplate.BiospecimenUidTemplateContainerPanel;
import au.org.theark.lims.web.component.biospecimenupload.BiospecimenUploadContainerPanel;
import au.org.theark.lims.web.component.bioupload.BioUploadContainerPanel;
import au.org.theark.lims.web.component.inventory.panel.InventoryContainerPanel;
import au.org.theark.lims.web.component.inventory.tree.TreeModel;

/**
 * 
 * @author cellis
 *
 */
public class LimsSubMenuTab extends AbstractArkTabPanel {

	private static final long	serialVersionUID	= -2495883342790152951L;

	@SpringBean(name = au.org.theark.core.Constants.ARK_COMMON_SERVICE)
	private IArkCommonService<Void>	iArkCommonService;

	@SpringBean(name = Constants.LIMS_INVENTORY_SERVICE)
	private IInventoryService					iInventoryService;
	
	private WebMarkupContainer			arkContextMarkup;
	private WebMarkupContainer			studyNameMarkup;
	private WebMarkupContainer			studyLogoMarkup;
	private DefaultTreeModel 			treeModel;
	private Study							study;
	
	public LimsSubMenuTab(String id, WebMarkupContainer arkContextMarkup, WebMarkupContainer studyNameMarkup, WebMarkupContainer studyLogoMarkup, DefaultTreeModel treeModel) {
		super(id);
		this.arkContextMarkup = arkContextMarkup;
		this.studyNameMarkup = studyNameMarkup;
		this.studyLogoMarkup = studyLogoMarkup;
		this.treeModel = new TreeModel(iArkCommonService, iInventoryService).createTreeModel();
		ArkSession.get().setNodeObject(null);
		buildTabs();
		

		
		Long sessionStudyId = (Long) SecurityUtils.getSubject().getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
		if(sessionStudyId != null) {
			//study = iArkCommonService.getStudy(sessionStudyId);
		}
	}

	public void buildTabs() {
		List<ITab> moduleSubTabsList = new ArrayList<ITab>();

		ArkModule arkModule = iArkCommonService.getArkModuleByName(au.org.theark.core.Constants.ARK_MODULE_LIMS);
		List<ArkFunction> arkFunctionList = iArkCommonService.getModuleFunction(arkModule);// Gets a list of ArkFunctions for the given Module

		for (final ArkFunction menuArkFunction : arkFunctionList) {
			moduleSubTabsList.add(new AbstractTab(new StringResourceModel(menuArkFunction.getResourceKey(), this, null)) {
				/**
				 * 
				 */
				private static final long	serialVersionUID	= 1L;

				@Override
				public Panel getPanel(String panelId) {
					return buildPanels(menuArkFunction, panelId);
				}
				
				@Override
				public boolean isVisible() {
					boolean flag = true;
					SecurityManager securityManager = ThreadContext.getSecurityManager();
					Subject currentUser = SecurityUtils.getSubject();
					
					if(menuArkFunction.getResourceKey().equalsIgnoreCase("tab.module.lims.barcodeprinter")) {
						// Barcode printer redundant
						flag = false;
					}
					else if(menuArkFunction.getResourceKey().equalsIgnoreCase("tab.module.lims.biospecimenuidtemplate") || 
							menuArkFunction.getResourceKey().equalsIgnoreCase("tab.module.lims.barcodeprinter") || 
							menuArkFunction.getResourceKey().equalsIgnoreCase("tab.module.lims.barcodelabel")) {

						// Only a Super Administrator or LIMS Administrator can see the biospecimenuidtemplate/barcodeprinter/barcodelabel tabs
						if (securityManager.hasRole(currentUser.getPrincipals(), au.org.theark.core.security.RoleConstants.ARK_ROLE_SUPER_ADMINISTATOR) ||
								securityManager.hasRole(currentUser.getPrincipals(), au.org.theark.core.security.RoleConstants.ARK_ROLE_LIMS_ADMINISTATOR)) {
							flag = currentUser.isAuthenticated();
						}
						else {
							flag = false;
						}
					}
					return super.isVisible() && flag;
				}
			});
		}

		ArkAjaxTabbedPanel moduleTabbedPanel = new ArkAjaxTabbedPanel(Constants.MENU_LIMS_SUBMENU, moduleSubTabsList);
		add(moduleTabbedPanel);
	}

	protected Panel buildPanels(final ArkFunction arkFunction, String panelId) {
		Panel panelToReturn = null;// Set
		processAuthorizationCache(au.org.theark.core.Constants.ARK_MODULE_LIMS, arkFunction);
		
		//Moved to Subject Menu
		/*
		if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_SUBJECT)) {
			panelToReturn = new SubjectContainerPanel(panelId, arkContextMarkup, studyNameMarkup, studyLogoMarkup, treeModel);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BIOSPECIMEN)) {
			panelToReturn = new BiospecimenContainerPanel(panelId, arkContextMarkup, studyNameMarkup, studyLogoMarkup, treeModel);
		}
		else*/ 
		//Inventory(1)
		if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_INVENTORY)) {
			panelToReturn = new InventoryContainerPanel(panelId, treeModel);
		}
		//LIMS Custom field category(2)
		else if(arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD_CATEGORY)){
			panelToReturn = new CustomFieldCategoryContainerPanel(panelId, true, iArkCommonService.getArkFunctionByName(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD_CATEGORY));
		}
		// To be merged(Biospecimen with collection)
		//LIMS custom field(3)
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD)) {
		// useCustomFieldDisplay = true
			panelToReturn = new CustomFieldContainerPanel(panelId, true, iArkCommonService.getArkFunctionByName(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD));
		}
		//LIMS custom field upload(4)
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD_UPLOAD)) {
			panelToReturn = new CustomFieldUploadContainerPanel(panelId, iArkCommonService.getArkFunctionByName(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD_UPLOAD));
		}
		//Biospeciman Upload(5)
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BIOSPECIMEN_UPLOAD)) {
			panelToReturn = new BiospecimenUploadContainerPanel(panelId, arkFunction);
		}
		//Bar code label(6)
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BARCODE_LABEL)) {
			panelToReturn = new BarcodeLabelContainerPanel(panelId, arkContextMarkup);
		}
		// To be merged(Biospecimen with collection)
		/*else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD)) {
			// useCustomFieldDisplay = true
			panelToReturn = new CustomFieldContainerPanel(panelId, true, 
					iArkCommonService.getArkFunctionByName(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_CUSTOM_FIELD));
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_CUSTOM_FIELD_UPLOAD)) {
			panelToReturn = new CustomFieldUploadContainerPanel(panelId, iArkCommonService.getArkFunctionByName(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_CUSTOM_FIELD_UPLOAD),"BiospecimenCustomFieldUpload");
		}*/
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_LIMS_COLLECTION_CUSTOM_DATA)) {
			//panelToReturn = new BioCollectionCustomDataContainerPanel(panelId).initialisePanel();
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BIOSPECIMEN_CUSTOM_DATA)) {
			//panelToReturn = new BiospecimenCustomDataContainerPanel(panelId).initialisePanel();
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BIOSPECIMENUID_TEMPLATE)) {
			//panelToReturn = new BiospecimenUidTemplateContainerPanel(panelId, arkContextMarkup);
		}
		//Bio Collection and Bio speciman upload.
		//else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_BIOSPECIMEN_AND_BIOCOLLECTION_CUSTOM_FIELD_UPLOAD)) {
			//panelToReturn = new BioUploadContainerPanel(panelId, arkFunction);
		//}
		else {
			// This shouldn't happen when all functions have been implemented
			panelToReturn = new EmptyPanel(panelId);
		}
		return panelToReturn;
	}

}