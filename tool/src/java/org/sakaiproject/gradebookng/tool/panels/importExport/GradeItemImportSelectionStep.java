package org.sakaiproject.gradebookng.tool.panels.importExport;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.model.ImportedGrade;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItemStatus;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chmaurer on 1/22/15.
 */
public class GradeItemImportSelectionStep extends Panel {

    private static final Logger log = Logger.getLogger(GradeItemImportSelectionStep.class);

    private String panelId;

    public GradeItemImportSelectionStep(String id, final ImportWizardModel importWizardModel) {
        super(id);
        this.panelId = id;

        final CheckGroup<ImportedGrade> group = new CheckGroup<ImportedGrade>("group", new ArrayList<ImportedGrade>());

        Form<?> form = new Form("form")
        {
            @Override
            protected void onSubmit()
            {
                info("selected grade(s): " + group.getDefaultModelObjectAsString());

                List<ProcessedGradeItem> processedGradeItems = (List<ProcessedGradeItem>)group.getDefaultModelObject();

                log.debug("Processed items: " + processedGradeItems.size());

                //Process the selected items into the create/update lists
                List<ProcessedGradeItem> itemsToUpdate = filterListByStatus(processedGradeItems,
                        Arrays.asList(ProcessedGradeItemStatus.STATUS_UPDATE, ProcessedGradeItemStatus.STATUS_NA));
                List<ProcessedGradeItem> itemsToCreate = filterListByStatus(processedGradeItems,
                        Arrays.asList(ProcessedGradeItemStatus.STATUS_NEW));

                log.debug("Filtered Update items: " + itemsToUpdate.size());
                log.debug("Filtered Create items: " + itemsToCreate.size());

                List<ProcessedGradeItem> gbItemsToCreate = new ArrayList<ProcessedGradeItem>();
                for (ProcessedGradeItem item : itemsToCreate) {
                    //Don't want comment items here
                    if (!"N/A".equals(item.getItemPointValue())) {
                        gbItemsToCreate.add(item);
                    }
                }

                log.debug("Actual items to create: " + gbItemsToCreate.size());

                //repaint panel
                Component newPanel = null;
                importWizardModel.setProcessedGradeItems(processedGradeItems);
                importWizardModel.setGbItemsToCreate(gbItemsToCreate);
                importWizardModel.setItemsToCreate(itemsToCreate);
                importWizardModel.setItemsToUpdate(itemsToUpdate);
                if (gbItemsToCreate.size() > 0) {
                    importWizardModel.setStep(1);
                    importWizardModel.setTotalSteps(gbItemsToCreate.size());
                    newPanel = new CreateGradeItemStep(panelId, importWizardModel);
                }
                else
                    newPanel = new GradeImportConfirmationStep(panelId, importWizardModel);
                newPanel.setOutputMarkupId(true);
                GradeItemImportSelectionStep.this.replaceWith(newPanel);

            }
        };
        add(form);
        form.add(group);

        Button backButton = new Button("backbutton") {
            @Override
            public void onSubmit() {
                log.debug("Clicking back button...");
                Component newPanel = new GradeImportUploadStep(panelId);
                newPanel.setOutputMarkupId(true);
                GradeItemImportSelectionStep.this.replaceWith(newPanel);
            }
        };
        backButton.setDefaultFormProcessing(false);
        group.add(backButton);

        group.add(new Button("nextbutton"));

        group.add(new CheckGroupSelector("groupselector"));
        ListView<ProcessedGradeItem> gradeList = new ListView<ProcessedGradeItem>("grades",
                importWizardModel.getProcessedGradeItems())
        {
            /**
             * @see org.apache.wicket.markup.html.list.ListView#populateItem(org.apache.wicket.markup.html.list.ListItem)
             */
            @Override
            protected void populateItem(ListItem<ProcessedGradeItem> item)
            {

                item.add(new Check<ProcessedGradeItem>("checkbox", item.getModel()));
                item.add(new Label("itemTitle",
                        new PropertyModel<String>(item.getDefaultModel(), "itemTitle")));
                item.add(new Label("itemPointValue", new PropertyModel<String>(item.getDefaultModel(),
                        "itemPointValue")));

                //Use the status code to look up the text representation
                PropertyModel<ProcessedGradeItemStatus> statusProp = new PropertyModel<ProcessedGradeItemStatus>(item.getDefaultModel(), "status");
                ProcessedGradeItemStatus status = statusProp.getObject();

                //For external items, set a different label and disable the control
                if (status.getStatusCode() == ProcessedGradeItemStatus.STATUS_EXTERNAL) {
                    item.add(new Label("status", new StringResourceModel("importExport.status." + status.getStatusCode(), statusProp, null, status.getStatusValue())));
                    item.setEnabled(false);
                    item.add(new AttributeModifier("class", "external"));
                } else {
                    item.add(new Label("status", getString("importExport.status." + status.getStatusCode())));
                }

                String naString = getString("importExport.selection.pointValue.na", new Model(), "N/A");
                if (naString.equals(item.getModelObject().getItemPointValue()))
                    item.add(new AttributeAppender("class", new Model<String>("comment"), " "));
            }

        };

        gradeList.setReuseItems(true);
        group.add(gradeList);

    }

    private List<ProcessedGradeItem> filterListByStatus(List<ProcessedGradeItem> gradeList, List<Integer> statuses) {
        List<ProcessedGradeItem> filteredList = new ArrayList<ProcessedGradeItem>();
        for (ProcessedGradeItem gradeItem : gradeList) {
            if (statuses.contains(gradeItem.getStatus().getStatusCode()))
                filteredList.add(gradeItem);
        }
        return filteredList;
    }

}
