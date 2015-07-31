package org.sakaiproject.gradebookng.tool.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.MaskType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.util.Temp;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.panels.AddGradeItemPanel;
import org.sakaiproject.gradebookng.tool.panels.AssignmentColumnHeaderPanel;
import org.sakaiproject.gradebookng.tool.panels.CategoryColumnCellPanel;
import org.sakaiproject.gradebookng.tool.panels.CategoryColumnHeaderPanel;
import org.sakaiproject.gradebookng.tool.panels.GradeItemCellPanel;
import org.sakaiproject.gradebookng.tool.panels.StudentNameCellPanel;
import org.sakaiproject.gradebookng.tool.panels.StudentNameColumnHeaderPanel;
import org.sakaiproject.gradebookng.tool.panels.ToggleGradeItemsToolbarPanel;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;

/**
 * Grades page
 * 
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class GradebookPage extends BasePage {
	
	private static final long serialVersionUID = 1L;

	public static final String UNCATEGORIZED = "Uncategorized";

	ModalWindow addGradeItemWindow;
	ModalWindow studentGradeSummaryWindow;
	ModalWindow updateUngradedItemsWindow;
	ModalWindow gradeLogWindow;
	ModalWindow gradeCommentWindow;
	ModalWindow deleteItemWindow;

	Form<Void> form;

	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	public GradebookPage() {
		disableLink(this.gradebookPageLink);
		
		//students cannot access this page
		if(role == GbRole.STUDENT) {
			throw new RestartResponseException(StudentPage.class);
		}
		
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		Temp.time("GradebookPage init", stopwatch.getTime());

		form = new Form<Void>("form");
		add(form);
		
		/**
		 * Note that SEMI_TRANSPARENT has a 100% black background and TRANSPARENT is overridden to 10% opacity
		 */
		addGradeItemWindow = new ModalWindow("addGradeItemWindow");
		addGradeItemWindow.setMaskType(MaskType.TRANSPARENT);
		addGradeItemWindow.setResizable(false);
		addGradeItemWindow.setUseInitialHeight(false);
		addGradeItemWindow.showUnloadConfirmation(false);
		form.add(addGradeItemWindow);
		
		studentGradeSummaryWindow = new ModalWindow("studentGradeSummaryWindow");
		studentGradeSummaryWindow.setMaskType(MaskType.SEMI_TRANSPARENT);
		studentGradeSummaryWindow.setResizable(false);
		studentGradeSummaryWindow.setUseInitialHeight(false);
		studentGradeSummaryWindow.setWidthUnit("%");
		studentGradeSummaryWindow.setInitialWidth(95);
		form.add(studentGradeSummaryWindow);
		
		updateUngradedItemsWindow = new ModalWindow("updateUngradedItemsWindow");
		updateUngradedItemsWindow.setMaskType(MaskType.TRANSPARENT);
		updateUngradedItemsWindow.setResizable(false);
		updateUngradedItemsWindow.setUseInitialHeight(true);
		form.add(updateUngradedItemsWindow);
		
		gradeLogWindow = new ModalWindow("gradeLogWindow");
		gradeLogWindow.setMaskType(MaskType.TRANSPARENT);
		gradeLogWindow.setResizable(false);
		gradeLogWindow.setUseInitialHeight(false);
		form.add(gradeLogWindow);
		
		gradeCommentWindow = new ModalWindow("gradeCommentWindow");
		gradeCommentWindow.setMaskType(MaskType.TRANSPARENT);
		gradeCommentWindow.setResizable(false);
		gradeCommentWindow.setUseInitialHeight(false);
		form.add(gradeCommentWindow);
		
		deleteItemWindow = new ModalWindow("deleteItemWindow");
		deleteItemWindow.setMaskType(MaskType.TRANSPARENT);
		deleteItemWindow.setResizable(false);
		deleteItemWindow.setUseInitialHeight(false);
		form.add(deleteItemWindow);
		
		AjaxButton addGradeItem = new AjaxButton("addGradeItem") {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				ModalWindow window = getAddGradeItemWindow();
				window.setContent(new AddGradeItemPanel(window.getContentId(), window));
				window.show(target);
			}
		};
		addGradeItem.setDefaultFormProcessing(false);
		form.add(addGradeItem);
		
		//first get any settings data from the session
		final GradebookUiSettings settings = this.getUiSettings();
				
        //get list of assignments. this allows us to build the columns and then fetch the grades for each student for each assignment from the map
        final List<Assignment> assignments = this.businessService.getGradebookAssignments();
		Temp.time("getGradebookAssignments", stopwatch.getTime());
        
        //get the grade matrix. It should be sorted if we have that info
        final List<GbStudentGradeInfo> grades = businessService.buildGradeMatrix(assignments, settings.getAssignmentSortOrder(), settings.getNameSortOrder(), settings.getGroupFilter());
        
		Temp.time("buildGradeMatrix", stopwatch.getTime());
		
		//if the grade matrix is null, we dont have any data
		//TODO finish this page. Test by creating a new site and going to the tool
		if(grades == null) {
			throw new RestartResponseException(NoDataPage.class);
		}
		

        final Map<String, List<Long>> categorizedAssignmentOrder = businessService.getCategorizedAssignmentsOrder();

        //this could potentially be a sortable data provider
        final ListDataProvider<GbStudentGradeInfo> studentGradeMatrix = new ListDataProvider<GbStudentGradeInfo>(grades);
        List<IColumn> cols = new ArrayList<IColumn>();
        
        //add an empty column that we can use as a handle for selecting the row
        AbstractColumn handleColumn = new AbstractColumn(new Model("")){

			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				cellItem.add(new EmptyPanel(componentId));
			}
			
			@Override
			public String getCssClass() {
				return "gb-row-selector";
			}
        };
        cols.add(handleColumn);
        
        //student name column
        AbstractColumn studentNameColumn = new AbstractColumn(new Model("")) {

        	@Override
        	public Component getHeader(String componentId) {
        		return new StudentNameColumnHeaderPanel(componentId, Model.of(settings.getNameSortOrder())); //pass in the sort
        	}
        	
        	@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				GbStudentGradeInfo studentGradeInfo = (GbStudentGradeInfo) rowModel.getObject();
				
				Map<String,Object> modelData = new HashMap<>();
				modelData.put("userId", studentGradeInfo.getStudentUuid());
				modelData.put("eid", studentGradeInfo.getStudentEid());
				modelData.put("firstName", studentGradeInfo.getStudentFirstName());
				modelData.put("lastName", studentGradeInfo.getStudentLastName());
				modelData.put("displayName", studentGradeInfo.getStudentDisplayName());
				modelData.put("nameSortOrder", settings.getNameSortOrder()); //pass in the sort
				
				cellItem.add(new StudentNameCellPanel(componentId, Model.ofMap(modelData)));
				cellItem.add(new AttributeModifier("data-studentUuid", studentGradeInfo.getStudentUuid()));
				
				//TODO may need a subclass of Item that does the onComponentTag override and then tag.setName("th");
			}
        	
        	@Override
			public String getCssClass() {
				return "gb-student-cell";
			}

        };
        
        cols.add(studentNameColumn);
        
        // course grade column, pull from the studentgrades model
        cols.add(new PropertyColumn(new ResourceModel("column.header.coursegrade"), "courseGrade"));
        
        
        //build the rest of the columns based on the assignment list       
        for(final Assignment assignment: assignments) {
        	
        	AbstractColumn column = new AbstractColumn(new Model("")) {

            	@Override
            	public Component getHeader(String componentId) {
            		AssignmentColumnHeaderPanel panel = new AssignmentColumnHeaderPanel(componentId, new Model<Assignment>(assignment));
            		
            		String category = assignment.getCategoryName();
            		
            		int order = -1;
            		if (categorizedAssignmentOrder.containsKey(category)) {
            			order = categorizedAssignmentOrder.get(category).indexOf(assignment.getId());
            		}
            		
            		panel.add(new AttributeModifier("data-category", category));
            		panel.add(new AttributeModifier("data-categorized-order", order));
    				return panel;
            	}

				@Override
				public String getCssClass() {
					return "gb-grade-item-column-cell";
				}
            	
            	@Override
				public void populateItem(Item cellItem, String componentId, IModel rowModel) {
            		GbStudentGradeInfo studentGrades = (GbStudentGradeInfo) rowModel.getObject();
            		
            		GbGradeInfo gradeInfo = studentGrades.getGrades().get(assignment.getId());
            		
            		Map<String,Object> modelData = new HashMap<>();
    				modelData.put("assignmentId", assignment.getId());
    				modelData.put("assignmentPoints", assignment.getPoints()); //TODO might be able to set some of this higher up and use a getter in the subclasses, so its not passed around so much. It's common to the assignment....
    				modelData.put("studentUuid", studentGrades.getStudentUuid());
    				modelData.put("isExternal", assignment.isExternallyMaintained());
    				modelData.put("gradeInfo", gradeInfo);
    				
    				cellItem.add(new GradeItemCellPanel(componentId, Model.ofMap(modelData)));
    				
    				cellItem.setOutputMarkupId(true);
				}   
            	
            	
            };
                                   
            cols.add(column);
        }
        
        //render the categories (TODO may be able to pass this list into the matrix to save another lookup in there)
        final List<CategoryDefinition> categories = this.businessService.getGradebookCategories();
        
        for(final CategoryDefinition category: categories) {
        	AbstractColumn column = new AbstractColumn(new Model("")) {

        		@Override
            	public Component getHeader(String componentId) {
            		CategoryColumnHeaderPanel panel = new CategoryColumnHeaderPanel(componentId, new Model<CategoryDefinition>(category));

            		panel.add(new AttributeModifier("data-category", category.getName()));
            		
            		return panel;
        		}
            	
            	@Override
    			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
    				GbStudentGradeInfo studentGrades = (GbStudentGradeInfo) rowModel.getObject();
    				
            		Double score = studentGrades.getCategoryAverages().get(category.getId());
            		
            		Map<String,Object> modelData = new HashMap<>();
    				modelData.put("score", score);
    				
    				cellItem.add(new CategoryColumnCellPanel(componentId, Model.ofMap(modelData)));
    				cellItem.setOutputMarkupId(true);
    			}
            	
            	@Override
    			public String getCssClass() {
    				return "gb-category-item-column-cell";
    			} 
        		
        	};
             
             cols.add(column);
         }
       
		Temp.time("all Columns added", stopwatch.getTime());
        
        //TODO make this AjaxFallbackDefaultDataTable
        DataTable table = new DataTable("table", cols, studentGradeMatrix, 100);
        table.addBottomToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar(table, null));
        table.add(new AttributeModifier("data-siteid", this.businessService.getCurrentSiteId()));
        form.add(table);

        // Populate the toolbar 
        Label gradeItemSummary = new Label("gradeItemSummary", new StringResourceModel("label.toolbar.gradeitemsummary", null, assignments.size() + categories.size(), assignments.size() + categories.size()));
        gradeItemSummary.setEscapeModelStrings(false);
        form.add(gradeItemSummary);

        AjaxButton toggleCategoriesToolbarItem = new AjaxButton("toggleCategoriesToolbarItem") {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                if (settings.isCategoriesEnabled()) {
                    add(new AttributeModifier("class", "on"));
                }
            }
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                settings.setCategoriesEnabled(!settings.isCategoriesEnabled());
                setUiSettings(settings);

                if (settings.isCategoriesEnabled()) {
                    add(new AttributeModifier("class", "on"));
                } else {
                    add(new AttributeModifier("class", ""));
                }
                target.add(this);
                target.appendJavaScript("sakai.gradebookng.spreadsheet.toggleCategories();");
            }
        };
        form.add(toggleCategoriesToolbarItem);

        //section and group dropdown
        List<GbGroup> groups = this.businessService.getSiteSectionsAndGroups();
        
        //add the default ALL group to the list
        groups.add(0, new GbGroup(null, getString("groups.all"), GbGroup.Type.ALL));
            
		final DropDownChoice<GbGroup> groupFilter = new DropDownChoice<GbGroup>("groupFilter", new Model<GbGroup>(), groups, new ChoiceRenderer<GbGroup>() {
		private static final long serialVersionUID = 1L;
		
			@Override
			public Object getDisplayValue(GbGroup g) {
				return g.getTitle();
			}
			
			@Override
			public String getIdValue(GbGroup g, int index) {
				return g.getId();
			}
			
		});
		
		groupFilter.add(new AjaxFormComponentUpdatingBehavior("onchange") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				
				GbGroup selected = (GbGroup) groupFilter.getDefaultModelObject();
				
				//store selected group (null ok)
				GradebookUiSettings settings = getUiSettings();
				settings.setGroupFilter(selected);
				setUiSettings(settings);
				
				//refresh
				setResponsePage(new GradebookPage());
			}
			
		});
				
		//set selected group, or first item in list
		groupFilter.setModelObject((settings.getGroupFilter() != null) ? settings.getGroupFilter() : groups.get(0));
        groupFilter.setNullValid(false);
        form.add(groupFilter);

        add(new ToggleGradeItemsToolbarPanel("gradeItemsTogglePanel", assignments));
        
		Temp.time("Gradebook page done", stopwatch.getTime());
	}
	
	
	/**
	 * Getters for panels to get at modal windows
	 * @return
	 */
	public ModalWindow getAddGradeItemWindow() {
		return this.addGradeItemWindow;
	}
	
	public ModalWindow getStudentGradeSummaryWindow() {
		return this.studentGradeSummaryWindow;
	}
	
	public ModalWindow getUpdateUngradedItemsWindow() {
		return this.updateUngradedItemsWindow;
	}
	
	public ModalWindow getGradeLogWindow() {
		return this.gradeLogWindow;
	}
	
	public ModalWindow getGradeCommentWindow() {
		return this.gradeCommentWindow;
	}

	public ModalWindow getDeleteItemWindow() {
		return this.deleteItemWindow;
	}

	/**
	 * Getter for the GradebookUiSettings. Used to store a few UI related settings for the current session only.
	 * 
	 * TODO move this to a helper
	 */
	public GradebookUiSettings getUiSettings() {
		
		GradebookUiSettings settings = (GradebookUiSettings) Session.get().getAttribute("GBNG_UI_SETTINGS");
		
		if(settings == null) {
			settings = new GradebookUiSettings();
		}
		
		return settings;
	}
	
	public void setUiSettings(GradebookUiSettings settings) {
		Session.get().setAttribute("GBNG_UI_SETTINGS", settings);
	}
	
	
	
}
