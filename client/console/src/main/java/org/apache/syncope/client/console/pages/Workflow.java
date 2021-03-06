/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.pages;

import java.io.File;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.client.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class Workflow extends BasePage {

    private static final long serialVersionUID = -8781434495150074529L;

    @SpringBean
    private WorkflowRestClient wfRestClient;

    public Workflow(final PageParameters parameters) {
        super(parameters);

        WebMarkupContainer noActivitiEnabledForUsers = new WebMarkupContainer("noActivitiEnabledForUsers");
        noActivitiEnabledForUsers.setOutputMarkupPlaceholderTag(true);
        add(noActivitiEnabledForUsers);

        WebMarkupContainer workflowDefContainer = new WebMarkupContainer("workflowDefContainer");
        workflowDefContainer.setOutputMarkupPlaceholderTag(true);

        if (wfRestClient.isActivitiEnabledForUsers()) {
            noActivitiEnabledForUsers.setVisible(false);
        } else {
            workflowDefContainer.setVisible(false);
        }

        BookmarkablePageLink<Void> activitiModeler =
                new BookmarkablePageLink<>("activitiModeler", ActivitiModelerPopupPage.class);
        activitiModeler.setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(activitiModeler, ENABLE, Entitlement.WORKFLOW_DEF_READ);
        workflowDefContainer.add(activitiModeler);
        // Check if Activiti Modeler directory is found
        boolean activitiModelerEnabled = false;
        try {
            String activitiModelerDirectory = WebApplicationContextUtils.getWebApplicationContext(
                    WebApplication.get().getServletContext()).getBean("activitiModelerDirectory", String.class);
            File baseDir = new File(activitiModelerDirectory);
            activitiModelerEnabled = baseDir.exists() && baseDir.canRead() && baseDir.isDirectory();
        } catch (Exception e) {
            LOG.error("Could not check for Activiti Modeler directory", e);
        }
        activitiModeler.setEnabled(activitiModelerEnabled);

        BookmarkablePageLink<Void> xmlEditor = new BookmarkablePageLink<>("xmlEditor", XMLEditorPopupPage.class);
        xmlEditor.setPopupSettings(new VeilPopupSettings().setHeight(480).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditor, ENABLE, Entitlement.WORKFLOW_DEF_READ);
        workflowDefContainer.add(xmlEditor);

        Image workflowDefDiagram = new Image("workflowDefDiagram", new Model<IResource>()) {

            private static final long serialVersionUID = -8457850449086490660L;

            @Override
            protected IResource getImageResource() {
                return new DynamicImageResource() {

                    private static final long serialVersionUID = 923201517955737928L;

                    @Override
                    protected byte[] getImageData(final IResource.Attributes attributes) {
                        return wfRestClient.isActivitiEnabledForUsers()
                                ? wfRestClient.getDiagram()
                                : new byte[0];
                    }
                };
            }
        };
        workflowDefContainer.add(workflowDefDiagram);

        MetaDataRoleAuthorizationStrategy.authorize(workflowDefContainer, ENABLE, Entitlement.WORKFLOW_DEF_READ);
        add(workflowDefContainer);
    }

}
