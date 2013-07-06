package au.org.theark.study.web.component.pedigree.form;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

import au.org.theark.core.security.ArkPermissionHelper;
import au.org.theark.core.vo.ArkCrudContainerVO;
import au.org.theark.core.web.component.AbstractDetailModalWindow;
import au.org.theark.study.model.vo.PedigreeVo;
import au.org.theark.study.web.component.pedigree.PedigreeDisplayPanel;

public class SearchForm extends Form<PedigreeVo> {
	
	protected FeedbackPanel			feedbackPanel;
	
	protected ArkCrudContainerVO arkCrudContainerVO;
   
//	private PageableListView<BillableItem>					listView;
//	
//	private CompoundPropertyModel<BillableItemVo>			cpmModel;
	
	protected AjaxButton fatherButton;
	protected AjaxButton motherButton;
	protected AjaxButton twinButton;
	protected AjaxButton viewButton;
	protected AjaxButton exportButton;
	
	protected AbstractDetailModalWindow modalWindow;

	public SearchForm(String id, ArkCrudContainerVO arkCrudContainerVO, FeedbackPanel feedBackPanel) {
		super(id);
		this.setMultiPart(true);
		this.arkCrudContainerVO = arkCrudContainerVO;
		this.feedbackPanel = feedBackPanel;
//		this.listView = listView;
//		
//		this.cpmModel=cpmModel;

		initialiseSearchForm();
		addSearchComponentsToForm();
		Long sessionStudyId = (Long) SecurityUtils.getSubject().getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
		disableSearchForm(sessionStudyId, "There is no study in context. Please select a Study.");
	}
	
	protected void addSearchComponentsToForm() {		
		add(fatherButton);
		add(motherButton);
		add(twinButton);
		add(viewButton);
		add(exportButton);
		add(modalWindow);
	}

	protected void initialiseSearchForm() {
		modalWindow = new AbstractDetailModalWindow("detailModalWindow") {
			
			@Override
			protected void onCloseModalWindow(AjaxRequestTarget target) {
				// TODO Auto-generated method stub
				
			}
		};
		
		fatherButton = new AjaxButton(au.org.theark.core.Constants.FATHER){
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//TODO
			}						
		};
		
		motherButton = new AjaxButton(au.org.theark.core.Constants.MOTHER){
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//TODO
			}						
		};
		
		twinButton = new AjaxButton(au.org.theark.core.Constants.TWIN){
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//TODO
			}						
		};
		
		viewButton = new AjaxButton(au.org.theark.core.Constants.VIEW){
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				modalWindow.setTitle("Pedigree View");
				modalWindow.setInitialHeight(300);
				modalWindow.setInitialWidth(400);
				modalWindow.setContent(new PedigreeDisplayPanel("content"));
				modalWindow.show(target);
				
			}						
		};
		
		exportButton = new AjaxButton(au.org.theark.core.Constants.EXPORT){
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//TODO
			}						
		};
		
	}
	
	protected void disableSearchForm(Long sessionId, String errorMessage) {
		if (ArkPermissionHelper.isModuleFunctionAccessPermitted()) {
			if (sessionId == null) {
				arkCrudContainerVO.getSearchPanelContainer().setEnabled(false);
				this.error(errorMessage);
			}
			else {
				arkCrudContainerVO.getSearchPanelContainer().setEnabled(true);
			}
		}
		else {
			arkCrudContainerVO.getSearchPanelContainer().setEnabled(false);
			arkCrudContainerVO.getSearchResultPanelContainer().setVisible(false);
			this.error(au.org.theark.core.Constants.MODULE_NOT_ACCESSIBLE_MESSAGE);
		}
	}
}