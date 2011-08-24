package ch.systemsx.sybit.crkwebui.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.InterfaceItem;
import model.InterfaceScoreItem;
import model.PDBScoreItem;
import ch.systemsx.sybit.crkwebui.client.controllers.MainController;
import ch.systemsx.sybit.crkwebui.client.gui.renderers.GridCellRendererFactory;
import ch.systemsx.sybit.crkwebui.client.model.InterfaceItemModel;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
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
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
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
	private Label pdbIdentifier;
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

		pdbIdentifier = new Label(MainController.CONSTANTS.info_panel_pdb_identifier() + 
								  ": " + 
								  mainController.getPdbScoreItem().getPdbName() +
								  " (" +
								  mainController.getPdbScoreItem().getSpaceGroup() +
								  ")");
		
		pdbIdentifier.addStyleName("pdb-identifier-label");
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
		// resultsGrid.setStyleAttribute("borderTop", "none");

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
		fillResultsGrid(mainController.getPdbScoreItem());
		
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
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();

		InterfaceItemModel model = new InterfaceItemModel();

		String columnOrder = mainController.getSettings().getGridProperties()
				.get("results_columns");

		String[] columns = null;

		if (columnOrder == null) {
			columns = new String[model.getPropertyNames().size()];

			Iterator<String> fieldsIterator = model.getPropertyNames()
					.iterator();

			int i = 0;

			while (fieldsIterator.hasNext()) {
				columns[i] = fieldsIterator.next();
				i++;
			}
		} else {
			columns = columnOrder.split(",");
		}

		if (columns != null) {
			initialColumnWidth = new HashMap<String, Integer>();
		}

		for (String columnName : columns) {
			boolean addColumn = true;

			String customAdd = mainController.getSettings().getGridProperties()
					.get("results_" + columnName + "_add");
			if ((customAdd != null) && (!customAdd.equals("yes")))
			{
				addColumn = false;
			}

			if (addColumn) 
			{
				boolean displayColumn = true;

				String customVisibility = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_visible");
				if (customVisibility != null) {
					if (!customVisibility.equals("yes")) {
						displayColumn = false;
					}
				}

				int columnWidth = 75;
				String customColumnWidth = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_width");
				if (customColumnWidth != null) {
					columnWidth = Integer.parseInt(customColumnWidth);
				}

				String customRenderer = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_renderer");

				GridCellRenderer<BaseModel> renderer = null;
				if ((customRenderer != null) && (!customRenderer.equals(""))) {
					renderer = GridCellRendererFactory.createGridCellRenderer(
							customRenderer, mainController);
				}

				String header = columnName;
				String customHeader = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_header");
				if (customHeader != null) {
					header = customHeader;
				}
				
				boolean isResizable = true;

				String customIsResizable = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_resizable");
				if (customIsResizable != null) {
					if (!customIsResizable.equals("yes")) {
						isResizable = false;
					}
				}
				
				String tootlip = mainController.getSettings()
						.getGridProperties()
						.get("results_" + columnName + "_tooltip");

				if (columnName.equals("METHODS")) {
					for (String method : mainController.getSettings()
							.getScoresTypes()) {
						ColumnConfig column = new ColumnConfig();
						column.setId(method);
						column.setHeader(method);
						column.setWidth(columnWidth);
						column.setAlignment(HorizontalAlignment.CENTER);
						column.setHidden(!displayColumn);
						
						column.setResizable(isResizable);

						if (renderer != null) {
							column.setRenderer(renderer);
						}
						
						if (tootlip != null) {
							column.setToolTip(tootlip);
						}

						initialColumnWidth.put(method, columnWidth);

						configs.add(column);
					}
				} else {
					ColumnConfig column = new ColumnConfig();
					column.setId(columnName);
					column.setHeader(header);
					column.setWidth(columnWidth);
					column.setAlignment(HorizontalAlignment.CENTER);
					column.setHidden(!displayColumn);
					
					column.setResizable(isResizable);

					if (renderer != null) {
						column.setRenderer(renderer);
					}
					
					if (tootlip != null) {
						column.setToolTip(tootlip);
					}

					initialColumnWidth.put(columnName, columnWidth);

					configs.add(column);
				}
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
		
		LayoutContainer showThumbnailPanelLocation = new LayoutContainer();
		showThumbnailPanelLocation.setBorders(false);
		
		VBoxLayout vBoxLayout = new VBoxLayout();
		vBoxLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);
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
		
		LayoutContainer viewerTypePanelLocation = new LayoutContainer();
		viewerTypePanelLocation.setBorders(false);

		vBoxLayout = new VBoxLayout();
		vBoxLayout.setVBoxLayoutAlign(VBoxLayoutAlign.RIGHT);

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
		optionsLocation.add(showThumbnailPanelLocation, new RowData(0.5, 1, new Margins(0)));
		optionsLocation.add(viewerTypePanelLocation, new RowData(0.5, 1, new Margins(0)));
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
		
		pdbIdentifier.setText(MainController.CONSTANTS.info_panel_pdb_identifier() + ": " + resultsData.getPdbName());
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

				for (String method : mainController.getSettings().getScoresTypes())
				{
					for(InterfaceScoreItem interfaceScoreItem : interfaceItem.getInterfaceScores())
					{
						if(interfaceScoreItem.getMethod().equals(method))
						{
							model.set(method, interfaceScoreItem.getCall());
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
				model.setFinalCall(interfaceItem.getFinalCall());
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
			float multiplier = (float)maxWidth / resultsGridWidthOfAllVisibleColumns;
			
			int nrOfColumn = resultsGrid.getColumnModel().getColumnCount();
			
			for (int i = 0; i < nrOfColumn; i++) 
			{
				resultsGrid.getColumnModel().setColumnWidth(i, (int)(initialColumnWidth.get(resultsGrid.getColumnModel().getColumn(i).getId()) * multiplier), true);
//				resultsGrid.getColumnModel().getColumn(i)
//						.setWidth((int)(initialColumnWidth.get(i) * multiplier));
			}
		} 
		else 
		{
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
	
	public String getCurrentViewType() 
	{
		return viewerTypeComboBox.getValue().getValue();
	}
	
//	private void createResultsGridContainerToolbar()
//	{
//		ToolBar resultsGridContainerToolbar = new ToolBar();
//		
//		showThumbnailCheckBox = new CheckBox();
//		showThumbnailCheckBox.setBoxLabel("Show thumbnails");
//		String thumbnailCookie = Cookies.getCookie("crkthumbnail");
//		if ((thumbnailCookie != null) && (thumbnailCookie.equals("yes"))) 
//		{
//			showThumbnailCheckBox.setValue(true);
//		} 
//		else
//		{
//			showThumbnailCheckBox.setValue(false);
//		}
//		
//		showThumbnailCheckBox.addListener(Events.Change, new Listener<FieldEvent>() {
//
//			@Override
//			public void handleEvent(FieldEvent event)
//			{
//				Cookies.setCookie("crkthumbnail", String.valueOf(showThumbnailCheckBox.getValue()));
//				
//				for(ColumnConfig column : resultsColumnModel.getColumns())
//				{
//					if(column.getId().equals("thumbnail"))
//					{
//						if(showThumbnailCheckBox.getValue())
//						{
//							column.setHidden(true);
//						}
//						else
//						{
//							column.setHidden(false);
//						}
//					}
//				}
//			}
//			
//		});
//		
//		viewerTypeComboBox = new SimpleComboBox<String>();
//		viewerTypeComboBox.setId("viewercombo");
//		viewerTypeComboBox.setTriggerAction(TriggerAction.ALL);
//		viewerTypeComboBox.setEditable(false);
//		viewerTypeComboBox.setFireChangeEventOnSetValue(true);
//		viewerTypeComboBox.setWidth(100);
//		viewerTypeComboBox.add("Local");
//		viewerTypeComboBox.add("Jmol");
//
//		String viewerCookie = Cookies.getCookie("crkviewer");
//		if (viewerCookie != null) {
//			viewerTypeComboBox.setSimpleValue(viewerCookie);
//		} else {
//			viewerTypeComboBox.setSimpleValue("Jmol");
//		}
//
//		mainController.setSelectedViewer(viewerTypeComboBox.getValue()
//				.getValue());
//
//		viewerTypeComboBox.setFieldLabel("View mode");
//		viewerTypeComboBox.addListener(Events.Change,
//				new Listener<FieldEvent>() {
//					public void handleEvent(FieldEvent be) {
//						Cookies.setCookie("crkviewer", viewerTypeComboBox
//								.getValue().getValue());
//						mainController.setSelectedViewer(viewerTypeComboBox
//								.getValue().getValue());
//					}
//				});
//
//		resultsGridContainerToolbar.add(showThumbnailCheckBox);
//		resultsGridContainerToolbar.add(new FillToolItem());
//		resultsGridContainerToolbar.add(new LabelToolItem("3D Viewer: "));  
//		resultsGridContainerToolbar.add(viewerTypeComboBox);
//		resultsGridContainer.setTopComponent(resultsGridContainerToolbar);
//	}
}
