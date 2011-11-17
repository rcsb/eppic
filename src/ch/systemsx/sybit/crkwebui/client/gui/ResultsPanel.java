package ch.systemsx.sybit.crkwebui.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.systemsx.sybit.crkwebui.client.controllers.MainController;
import ch.systemsx.sybit.crkwebui.client.model.InterfaceItemModel;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceItem;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceScoreItem;
import ch.systemsx.sybit.crkwebui.shared.model.PDBScoreItem;
import ch.systemsx.sybit.crkwebui.shared.model.SupportedMethod;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.user.client.Cookies;

/**
 * This panel is used to display the results of the calculations
 * @author srebniak_a
 *
 */
public class ResultsPanel extends DisplayPanel 
{
	private PDBIdentifierLabel pdbIdentifier;
	private Label pdbTitle;
	
	private InfoPanel infoPanel;
	
	private CheckBox showThumbnailCheckBox;
	private SimpleComboBox<String> viewerTypeComboBox;
	
	// ***************************************
	// * Results grid
	// ***************************************
	private ContentPanel resultsGridContainer;
	private Grid<InterfaceItemModel> resultsGrid;
	private List<ColumnConfig> resultsConfigs;
	private ListStore<InterfaceItemModel> resultsStore;
	private ColumnModel resultsColumnModel;
	private Map<String, Integer> initialColumnWidth;
	
	private float gridWidthMultiplier;
	// ***************************************

	// ***************************************
	// * Scores grid
	// ***************************************
//	private LayoutContainer scoresPanelLocation;
//	private ScoresPanel scoresPanel;
	// ***************************************

	public ResultsPanel(final MainController mainController)
	{
		super(mainController);
		this.setBorders(true);
		this.setLayout(new RowLayout(Orientation.VERTICAL));
		this.setStyleAttribute("padding", "10px");

		pdbIdentifier = new PDBIdentifierLabel();
		pdbIdentifier.setPDBText(mainController.getPdbScoreItem().getPdbName(),
								 mainController.getPdbScoreItem().getSpaceGroup());
		this.add(pdbIdentifier);
		
		FormPanel breakPanel = new FormPanel();
		breakPanel.setBorders(false);
		breakPanel.setBodyBorder(false);
		breakPanel.setPadding(0);
		breakPanel.getHeader().setVisible(false);
		this.add(breakPanel, new RowData(1, 1.1, new Margins(0)));
		
		pdbTitle = new Label(mainController.getPdbScoreItem().getTitle());
		pdbTitle.addStyleName("crk-default-label");
		this.add(pdbTitle);
		
		breakPanel = new FormPanel();
		breakPanel.setBorders(false);
		breakPanel.setBodyBorder(false);
		breakPanel.setPadding(0);
		breakPanel.getHeader().setVisible(false);
		this.add(breakPanel, new RowData(1, 10, new Margins(0)));
		
		createInfoPanel();

		createViewerTypePanel();

		breakPanel = new FormPanel();
		breakPanel.setBorders(false);
		breakPanel.setBodyBorder(false);
		breakPanel.setPadding(0);
		breakPanel.getHeader().setVisible(false);
		this.add(breakPanel, new RowData(1, 5, new Margins(0)));
		
		resultsConfigs = createColumnConfig();
		
		if(!showThumbnailCheckBox.getValue())
		{
			for(ColumnConfig column : resultsConfigs)
			{
				if(column.getId().equals("thumbnail"))
				{
					column.setHidden(true);
				}
			}
		}

		resultsStore = new ListStore<InterfaceItemModel>();
		resultsColumnModel = new ColumnModel(resultsConfigs);

		resultsGrid = new Grid<InterfaceItemModel>(resultsStore, resultsColumnModel);
		resultsGrid.getView().setForceFit(false);

		resultsGrid.setBorders(false);
		resultsGrid.setStripeRows(true);
		resultsGrid.setColumnLines(true);
		resultsGrid.setColumnReordering(true);

//		Listener<GridEvent> resultsGridListener = new Listener<GridEvent>() {
//			@Override
//			public void handleEvent(GridEvent event) 
//			{
//				updateScoresPanel((Integer) resultsStore.getAt(event.getRowIndex()).get("id"));
//			}
//		};
//		
//		resultsGrid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
//		resultsGrid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<BeanModel>() 
//		{
//			@Override
//			public void selectionChanged(SelectionChangedEvent<BeanModel> se) 
//			{
//				if(resultsGrid.getSelectionModel().getSelectedItem() != null)
//				{
//					updateScoresPanel((Integer) resultsGrid.getSelectionModel().getSelectedItem().get("id"));
//				}
//			}
//			
//		});
		
//		resultsGrid.addListener(Events.CellClick, resultsGridListener);
		resultsGrid.setContextMenu(new ResultsPanelContextMenu(mainController));
		resultsGrid.disableTextSelection(false);
//		resultsGrid.setAutoHeight(true);
//		fillResultsGrid(mainController.getPdbScoreItem());
		
//		resultsGrid.addListener(Events.ColumnResize, new Listener<BaseEvent>(){
//			@Override
//			public void handleEvent(BaseEvent be) {
//				mainController.resizeResultsGrid();
//			}
//		});
//		
//		resultsGrid.addListener(Events.ColumnMove, new Listener<BaseEvent>(){
//			@Override
//			public void handleEvent(BaseEvent be) {
//				mainController.resizeResultsGrid();
//			}
//		});
		
		resultsGrid.addListener(Events.ContextMenu, new Listener<BaseEvent>(){
			@Override
			public void handleEvent(BaseEvent be) {
				mainController.resizeResultsGrid();
			}
		});
		
		resultsGrid.addListener(Events.ColumnResize, new Listener<GridEvent>() {
			@Override
			public void handleEvent(GridEvent ge) 
			{
				int widthToSet = (int) (ge.getWidth() / gridWidthMultiplier);
				mainController.getSettings().getGridProperties().put("results_" + resultsGrid.getColumnModel().getColumnId(ge.getColIndex()) + "_width", 
																	 String.valueOf(widthToSet));
			}
		});
		
		new KeyNav<ComponentEvent>(resultsGrid)
		{
			@Override
            public void onEnter(ComponentEvent ce) 
			{
				InterfaceItemModel interfaceItemModel = resultsGrid.getSelectionModel().getSelectedItem();
				if(interfaceItemModel != null)
				{
					mainController.getInterfaceResidues(interfaceItemModel.getId());
				}
			}
		};

		resultsGridContainer = new ContentPanel();
		resultsGridContainer.getHeader().setVisible(false);
		resultsGridContainer.setBorders(true);
		resultsGridContainer.setBodyBorder(false);
		resultsGridContainer.setLayout(new FitLayout());
//		resultsGridContainer.setScrollMode(Scroll.AUTO);
		resultsGridContainer.add(resultsGrid);
		
		this.add(resultsGridContainer, new RowData(1, 1, new Margins(0)));
		
//		createResultsGridContainerToolbar();

//		breakPanel = new FormPanel();
//		breakPanel.setBorders(false);
//		breakPanel.setBodyBorder(false);
//		breakPanel.setPadding(0);
//		breakPanel.getHeader().setVisible(false);
//		this.add(breakPanel, new RowData(1, 10, new Margins(0)));
//
//		scoresPanelLocation = new LayoutContainer();
//		scoresPanelLocation.setLayout(new FitLayout());
//		scoresPanelLocation.setBorders(false);
//		
//		this.add(scoresPanelLocation, new RowData(1, 0.45, new Margins(0)));
	}

//	public void updateScoresPanel(int selectedInterface)
//	{
//		if (scoresPanel == null) 
//		{
//			createScoresPanel();
//			scoresPanelLocation.add(scoresPanel);
//			scoresPanelLocation.layout();
//		}
//		
//		scoresPanel.fillGrid(mainController.getPdbScoreItem(), selectedInterface);
//		scoresPanel.resizeGrid();
//		scoresPanel.setVisible(true);
//	}

	private List<ColumnConfig> createColumnConfig() 
	{
		List<ColumnConfig> configs = GridColumnConfigGenerator.createColumnConfigs(mainController,
																				   "results",
																				   new InterfaceItemModel());

		if(configs != null)
		{
			initialColumnWidth = new HashMap<String, Integer>();
			
			for(ColumnConfig columnConfig : configs)
			{
				initialColumnWidth.put(columnConfig.getId(), columnConfig.getWidth());
			}
		}

		return configs;
	}

	private void createInfoPanel()
	{
		infoPanel = new InfoPanel(mainController);
		this.add(infoPanel, new RowData(1, 80, new Margins(0)));
	}

	private void createViewerTypePanel() 
	{
		LayoutContainer optionsLocation = new LayoutContainer();
		optionsLocation.setLayout(new RowLayout(Orientation.HORIZONTAL));
		optionsLocation.setStyleAttribute("padding-top", "10px");

		LayoutContainer viewerTypePanelLocation = new LayoutContainer();
		viewerTypePanelLocation.setBorders(false);

		VBoxLayout vBoxLayout = new VBoxLayout();
		vBoxLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);

		viewerTypePanelLocation.setLayout(vBoxLayout);

		FormPanel viewerTypePanel = new FormPanel();
		viewerTypePanel.getHeader().setVisible(false);
		viewerTypePanel.setBorders(false);
		viewerTypePanel.setBodyBorder(false);
		viewerTypePanel.setFieldWidth(100);
		viewerTypePanel.setPadding(0);

		viewerTypeComboBox = new SimpleComboBox<String>();
		viewerTypeComboBox.setId("viewercombo");
		viewerTypeComboBox.setTriggerAction(TriggerAction.ALL);
		viewerTypeComboBox.setEditable(false);
		viewerTypeComboBox.setFireChangeEventOnSetValue(true);
		viewerTypeComboBox.setWidth(100);
		viewerTypeComboBox.add(MainController.CONSTANTS.viewer_local());
		viewerTypeComboBox.add(MainController.CONSTANTS.viewer_jmol());
		viewerTypeComboBox.add(MainController.CONSTANTS.viewer_pse());

		String viewerCookie = Cookies.getCookie("crkviewer");
		if (viewerCookie != null) {
			viewerTypeComboBox.setSimpleValue(viewerCookie);
		} else {
			viewerTypeComboBox.setSimpleValue(MainController.CONSTANTS.viewer_jmol());
		}

		mainController.setSelectedViewer(viewerTypeComboBox.getValue()
				.getValue());

		viewerTypeComboBox.setFieldLabel(MainController.CONSTANTS.results_grid_viewer_combo_label());
		viewerTypeComboBox.setLabelStyle("crk-default-label");
		viewerTypeComboBox.addListener(Events.Change,
				new Listener<FieldEvent>() {
					public void handleEvent(FieldEvent be) {
						Cookies.setCookie("crkviewer", viewerTypeComboBox
								.getValue().getValue());
						mainController.setSelectedViewer(viewerTypeComboBox
								.getValue().getValue());
					}
				});

		viewerTypePanel.add(viewerTypeComboBox);
		viewerTypePanelLocation.add(viewerTypePanel);
		
		LayoutContainer showThumbnailPanelLocation = new LayoutContainer();
		showThumbnailPanelLocation.setBorders(false);
		
		vBoxLayout = new VBoxLayout();
		vBoxLayout.setVBoxLayoutAlign(VBoxLayoutAlign.RIGHT);
		showThumbnailPanelLocation.setLayout(vBoxLayout);
		
		showThumbnailCheckBox = new CheckBox();
		showThumbnailCheckBox.setBoxLabel(MainController.CONSTANTS.results_grid_show_thumbnails());
		displayThumbnails();
		showThumbnailCheckBox.addListener(Events.Change, new Listener<FieldEvent>() {

			@Override
			public void handleEvent(FieldEvent event)
			{
				Cookies.setCookie("crkthumbnail", String.valueOf(showThumbnailCheckBox.getValue()));
				
				for(ColumnConfig column : resultsGrid.getColumnModel().getColumns())
				{
					if(column.getId().equals("thumbnail"))
					{
						if(showThumbnailCheckBox.getValue())
						{
							column.setHidden(false);
						}
						else
						{
							column.setHidden(true);
						}
						
						resizeGrid();
					}
				}
			}
			
		});
		
		showThumbnailPanelLocation.add(showThumbnailCheckBox);

		optionsLocation.add(viewerTypePanelLocation, new RowData(0.5, 1, new Margins(0)));
		optionsLocation.add(showThumbnailPanelLocation, new RowData(0.5, 1, new Margins(0)));
		this.add(optionsLocation, new RowData(1, 35, new Margins(0)));
	}

//	private void createScoresPanel() 
//	{
//		scoresPanel = new ScoresPanel(mainController);
//	}

	public void fillResultsPanel(PDBScoreItem resultsData) 
	{
//		if (scoresPanel != null)
//		{
//			scoresPanelLocation.removeAll();
//			scoresPanel = null;
//		}

		fillResultsGrid(resultsData);
		infoPanel.generateInfoPanel(mainController);
		
		pdbIdentifier.setPDBText(resultsData.getPdbName(),
							  	 resultsData.getSpaceGroup());
		
		pdbTitle.setText(resultsData.getTitle());
	}
	
	public void fillResultsGrid(PDBScoreItem resultsData)
	{
		boolean hideWarnings = true;
		
		resultsStore.removeAll();

		List<InterfaceItemModel> data = new ArrayList<InterfaceItemModel>();

		List<InterfaceItem> interfaceItems = resultsData.getInterfaceItems();

		if (interfaceItems != null)
		{
			for (InterfaceItem interfaceItem : interfaceItems) 
			{
				if((interfaceItem.getWarnings() != null) &&
				   (interfaceItem.getWarnings().size() > 0))
			    {
					hideWarnings = false;
			    }
				
				InterfaceItemModel model = new InterfaceItemModel();

				for (SupportedMethod method : mainController.getSettings().getScoresTypes())
				{
					for(InterfaceScoreItem interfaceScoreItem : interfaceItem.getInterfaceScores())
					{
						if(interfaceScoreItem.getMethod().equals(method.getName()))
						{
							model.set(method.getName(), interfaceScoreItem.getCallName());
						}
					}
				}
				
				// Window.alert(String.valueOf(interfaceItem));
				// ResultsModel resultsModel = new ResultsModel("");
				model.setId(interfaceItem.getId());
				model.setName(interfaceItem.getName());
				model.setArea(interfaceItem.getArea());
				model.setSize1(interfaceItem.getSize1());
				model.setSize2(interfaceItem.getSize2());
				model.setFinalCallName(interfaceItem.getFinalCallName());
				model.setOperator(interfaceItem.getOperator());
				model.setWarnings(interfaceItem.getWarnings());

				data.add(model);
			}
		}

		resultsStore.add(data);
		
		boolean resizeGrid = false;
		if(resultsColumnModel.getColumnById("warnings").isHidden() != hideWarnings)
		{
			resizeGrid = true;
		}
		
		resultsColumnModel.getColumnById("warnings").setHidden(hideWarnings);
		
		resultsGrid.reconfigure(resultsStore, resultsColumnModel);
		
		if(resizeGrid)
		{
			resizeGrid();
		}
	}

	public void resizeGrid() 
	{
		int limit = 50;
		if(mainController.getMainViewPort().getMyJobsPanel().isExpanded())
		{
			limit += mainController.getMainViewPort().getMyJobsPanel().getWidth();
		}
		else
		{
			limit += 25;
		}
		
		int resultsGridWidthOfAllVisibleColumns = 0;
		
		for(int i=0; i<resultsGrid.getColumnModel().getColumnCount(); i++)
		{
			if(!resultsGrid.getColumnModel().getColumn(i).isHidden())
			{
				resultsGridWidthOfAllVisibleColumns += initialColumnWidth.get(resultsGrid.getColumnModel().getColumn(i).getId());
			}
		}
		
		if (resultsGridWidthOfAllVisibleColumns < mainController.getWindowWidth() - limit) 
		{
			int maxWidth = mainController.getWindowWidth() - limit - 20;
			gridWidthMultiplier = (float)maxWidth / resultsGridWidthOfAllVisibleColumns;
			
			int nrOfColumn = resultsGrid.getColumnModel().getColumnCount();
			
			for (int i = 0; i < nrOfColumn; i++) 
			{
				resultsGrid.getColumnModel().setColumnWidth(i, (int)(initialColumnWidth.get(resultsGrid.getColumnModel().getColumn(i).getId()) * gridWidthMultiplier), true);
//				resultsGrid.getColumnModel().getColumn(i)
//						.setWidth((int)(initialColumnWidth.get(i) * multiplier));
			}
		} 
		else 
		{
			gridWidthMultiplier = 1;
			
			int nrOfColumn = resultsGrid.getColumnModel().getColumnCount();

			for (int i = 0; i < nrOfColumn; i++) {
				resultsGrid.getColumnModel().getColumn(i)
						.setWidth(initialColumnWidth.get(resultsGrid.getColumnModel().getColumn(i).getId()));
			}
		}
		
		resultsGrid.setWidth(mainController.getWindowWidth() - limit);

//		resultsGrid.reconfigure(resultsStore, resultsColumnModel);
		resultsGrid.getView().refresh(true);
		resultsGrid.getView().layout();
		resultsGrid.repaint();
		
		
		this.layout();
		
		if(resultsGrid.getView().getHeader() != null)
		{
			resultsGrid.getView().getHeader().refresh();
		}
	}

//	public void resizeScoresGrid() 
//	{
//		if(scoresPanel != null)
//		{
//			scoresPanel.resizeGrid();
//		}
//	}
	
	public void displayThumbnails()
	{
		String thumbnailCookie = Cookies.getCookie("crkthumbnail");
		if(thumbnailCookie == null)
		{
			thumbnailCookie = "true";
		}
		
		if (thumbnailCookie.equals("true"))
		{
			showThumbnailCheckBox.setValue(true);
		} 
		else
		{
			showThumbnailCheckBox.setValue(false);
		}
	}
	
	public InfoPanel getInfoPanel() 
	{
		return infoPanel;
	}
	
//	public ScoresPanel getScoresPanel()
//	{
//		return scoresPanel;
//	}
	
	public Grid<InterfaceItemModel> getResultsGrid()
	{
		return resultsGrid;
	}
	
	public ListStore<InterfaceItemModel> getResultsStore() 
	{
		return resultsStore;
	}

	public void saveGridSettings()
	{
		for(int i=0; i<resultsGrid.getColumnModel().getColumnCount(); i++)
		{
			String value = mainController.getSettings().getGridProperties().get("results_" + resultsGrid.getColumnModel().getColumn(i).getId() + "_visible");
			
			if(resultsGrid.getColumnModel().getColumn(i).isHidden() && ((value == null) || (!value.equals("no"))))
			{
				mainController.getSettings().getGridProperties().put("results_" + resultsGrid.getColumnModel().getColumn(i).getId() + "_visible", 
																	 "no");	
			}
			else if(!resultsGrid.getColumnModel().getColumn(i).isHidden() && (value != null) && (!value.equals("yes")))
			{
				mainController.getSettings().getGridProperties().put("results_" + resultsGrid.getColumnModel().getColumn(i).getId() + "_visible", 
																	 "yes");	
			}
		}
	}
}
