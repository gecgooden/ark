package au.org.theark.core.web.component.checkbox;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.model.IModel;

public abstract class ArkConfirmCheckBox extends AjaxCheckBox implements IAjaxIndicatorAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private final AjaxIndicatorAppender indicatorAppender = new AjaxIndicatorAppender();
	
	IModel<String> confirm;

	public ArkConfirmCheckBox(String id, IModel confirm, IModel<Boolean> value) {
		this(id,value);
		this.confirm=confirm;
		// TODO Auto-generated constructor stub
	}

	public ArkConfirmCheckBox(String id, IModel<Boolean> model) {
		super(id, model);
		add(indicatorAppender);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getAjaxIndicatorMarkupId() {
		// TODO Auto-generated method stub
		return indicatorAppender.getMarkupId();
	}
	
	@Override
	protected IAjaxCallDecorator getAjaxCallDecorator() {
		return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {
			private static final long	serialVersionUID	= 1L;

			@Override
			public CharSequence preDecorateScript(CharSequence script) {
				return "if(!confirm('" + confirm.getObject() + "'))" + "{ " + "	return false " + "} " + "else " + "{ " + "	this.disabled = true; " + "};" + script;
			}			
			
		};
	}

	@Override
	protected abstract void onUpdate(AjaxRequestTarget target);
	
	
	
	
	
}
