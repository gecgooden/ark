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
package au.org.theark.lims.web.component.subjectlims.lims.biocollection;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import au.org.theark.core.vo.ArkCrudContainerVO;
import au.org.theark.core.vo.LimsVO;
import au.org.theark.lims.web.component.subjectlims.lims.biocollection.form.BioCollectionDataEntryModalDetailForm;

public class BioCollectionDataEntryModalDetailPanel extends Panel {

	private static final long						serialVersionUID	= -8745753185256494362L;

	private FeedbackPanel							detailFeedbackPanel;
	private ModalWindow								modalWindow;
	private BioCollectionDataEntryModalDetailForm			detailForm;
	private ArkCrudContainerVO						arkCrudContainerVo;

	protected CompoundPropertyModel<LimsVO>	cpModel;

	public BioCollectionDataEntryModalDetailPanel(String id, ModalWindow modalWindow, CompoundPropertyModel<LimsVO> cpModel) {
		super(id);
		this.detailFeedbackPanel = initialiseFeedBackPanel();
		this.setModalWindow(modalWindow);
		this.arkCrudContainerVo = new ArkCrudContainerVO();
		this.cpModel = cpModel;
		initialisePanel();
	}

	protected FeedbackPanel initialiseFeedBackPanel() {
		/* Feedback Panel */
		detailFeedbackPanel = new FeedbackPanel("detailFeedback");
		detailFeedbackPanel.setOutputMarkupId(true);
		return detailFeedbackPanel;
	}

	public void initialisePanel() {
		detailForm = new BioCollectionDataEntryModalDetailForm("detailForm", detailFeedbackPanel, arkCrudContainerVo, modalWindow, cpModel);
		detailForm.initialiseDetailForm();
		add(detailFeedbackPanel);
		add(detailForm);
	}

	/**
	 * @param modalWindow
	 *           the modalWindow to set
	 */
	public void setModalWindow(ModalWindow modalWindow) {
		this.modalWindow = modalWindow;
	}

	/**
	 * @return the modalWindow
	 */
	public ModalWindow getModalWindow() {
		return modalWindow;
	}

}
