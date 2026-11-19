/*-
 * #%L
 * Carnet de pêche :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import ChartDataLabels from "chartjs-plugin-datalabels";

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Legend, Tooltip, ChartDataLabels);

/** En dessous de cette largeur, les valeurs des barres s'écrivent à la verticale. */
const NARROW_CHART_WIDTH_PX = 480;

/** Part minimale d'une part de camembert pour y écrire son pourcentage. */
const PIE_LABEL_MIN_SHARE = 0.06;

/**
 * Options communes des graphiques Chart.js des statistiques pêcheur : légende
 * cliquable qui masque ou réaffiche une série (ou une part) et valeurs écrites
 * sur le graphique, lisibles sur smartphone sans toucher l'écran.
 */
export default class CarnetCharts {
  /**
   * Légende cliquable : un appui masque la série (ou la part), un second la
   * réaffiche, avec un pointeur au survol. Le titre d'un axe disparaît avec ses
   * séries.
   *
   * @param position emplacement de la légende
   */
  static toggleableLegend(position: "top" | "bottom" | "right" = "top"): any {
    return {
      display: true,
      position,
      labels: { usePointStyle: true, boxWidth: 10, padding: 14, font: { size: 12, weight: 600 } },
      onClick(this: any, event: any, item: any, legend: any) {
        CarnetCharts.defaultLegendClick(legend.chart).call(this, event, item, legend);
        CarnetCharts.syncAxisTitles(legend.chart);
      },
      onHover: (event: any) => CarnetCharts.setCursor(event, "pointer"),
      onLeave: (event: any) => CarnetCharts.setCursor(event, "default"),
    };
  }

  /**
   * Valeur écrite au-dessus de chaque barre non nulle, à la verticale sur un
   * écran étroit pour tenir au-dessus de barres fines. Masquer une série dans la
   * légende libère la place.
   *
   * @param format mise en forme de la valeur
   */
  static barValueLabels(format: (value: number) => string = CarnetCharts.frenchNumber): any {
    return {
      anchor: "end",
      align: "end",
      offset: 2,
      padding: 0,
      clamp: true,
      font: { size: 11, weight: "bold" },
      rotation: (context: any) => (CarnetCharts.isNarrow(context) ? -90 : 0),
      // À la verticale, des valeurs voisines se touchent sans se chevaucher : on
      // les garde toutes ; à l'horizontale, Chart.js masque celles qui se chevauchent.
      display: (context: any) => {
        if (!(context.dataset.data[context.dataIndex] > 0)) {
          return false;
        }
        return CarnetCharts.isNarrow(context) ? true : "auto";
      },
      formatter: (value: number) => format(value),
    };
  }

  /** Nombre au format français, une décimale au plus : « 12,5 ». */
  static frenchNumber(value: number): string {
    return value.toLocaleString("fr-FR", { maximumFractionDigits: 1 });
  }

  /** Pourcentage écrit dans chaque part assez grande d'un camembert. */
  static pieShareLabels(): any {
    return {
      color: "white",
      font: { size: 12, weight: "bold" },
      display: (context: any) => CarnetCharts.visibleShare(context) >= PIE_LABEL_MIN_SHARE,
      formatter: (_value: number, context: any) => `${Math.round(CarnetCharts.visibleShare(context) * 100)} %`,
    };
  }

  /**
   * Axe affiché seulement si l'une de ses séries est visible : masquer une série
   * dans la légende retire aussi son axe et le graphique s'adapte.
   */
  static autoHiddenAxis(axis: any): any {
    return { display: "auto", grace: "20%", ...axis };
  }

  /** Bascule par défaut : la série pour les barres, la part pour un camembert. */
  private static defaultLegendClick(chart: any): (...args: any[]) => void {
    const overrides = (ChartJS.overrides as any)[chart.config.type];
    const override = overrides && overrides.plugins && overrides.plugins.legend && overrides.plugins.legend.onClick;
    return override || ChartJS.defaults.plugins.legend.onClick;
  }

  /** Affiche le titre d'un axe seulement si l'une de ses séries est visible. */
  private static syncAxisTitles(chart: any) {
    const scales = chart.options.scales || {};
    Object.keys(scales).forEach((axisId) => {
      const title = scales[axisId].title;
      if (!title || !title.text) {
        return;
      }
      title.display = chart.data.datasets.some(
        (dataset: any, index: number) =>
          chart.isDatasetVisible(index) && (dataset.yAxisID === axisId || dataset.xAxisID === axisId)
      );
    });
    chart.update();
  }

  private static isNarrow(context: any): boolean {
    return context.chart.width < NARROW_CHART_WIDTH_PX;
  }

  /** Part de la valeur parmi les parts visibles (les parts masquées sont exclues). */
  private static visibleShare(context: any): number {
    const chart = context.chart;
    const data: number[] = context.dataset.data;
    const total = data.reduce(
      (sum, value, index) => (chart.getDataVisibility(index) ? sum + (Number(value) || 0) : sum),
      0
    );
    return total > 0 ? (Number(data[context.dataIndex]) || 0) / total : 0;
  }

  private static setCursor(event: any, cursor: string) {
    const canvas = event && event.native && (event.native.target as HTMLElement);
    if (canvas) {
      canvas.style.cursor = cursor;
    }
  }
}
