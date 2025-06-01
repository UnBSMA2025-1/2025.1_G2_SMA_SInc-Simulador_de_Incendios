import cv2
import numpy as np
import json
import argparse
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
from PIL import Image
import os

class ImageToMapConverter:
    def __init__(self):
        self.tile_types = {
            0: "no_vegetation",    # sem vegetação
            1: "dry_vegetation",   # vegetação seca
            2: "wet_vegetation",   # vegetação úmida
            3: "common_vegetation", # vegetação comum
            4: "on_fire",          # em chamas (not used in generation)
            5: "burnt",            # queimado (not used in generation)
            6: "water"             # água
        }

        self.color_ranges = {
            'water': {
                'lower': np.array([90, 50, 70]),    # Azul claro até
                'upper': np.array([130, 255, 255])  # Azul forte
            },
            'dry_vegetation': {
                'lower': np.array([10, 60, 50]),    # Marrom amarelado
                'upper': np.array([25, 255, 255])   # Palha seca
            },
            'wet_vegetation': {
                'lower': np.array([36, 80, 60]),    # Verde vibrante
                'upper': np.array([85, 255, 255])   # Verde escuro molhado
            },
            'no_vegetation': {
                'lower': np.array([0, 0, 100]),     # Tons de cinza a branco
                'upper': np.array([180, 30, 255])   # Inclui concreto, solo claro
            }
        }


    def preprocess_image(self, image_path, target_width, target_height):
        """Load and preprocess the image"""
        # Load image
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Could not load image: {image_path}")

        # Resize to match map dimensions
        resized = cv2.resize(image, (target_width, target_height), interpolation=cv2.INTER_AREA)

        # Convert to HSV for better color segmentation
        hsv = cv2.cvtColor(resized, cv2.COLOR_BGR2HSV)

        return resized, hsv

    def classify_pixel_by_color(self, hsv_pixel):
        """Classify a single pixel based on color ranges"""
        if self._pixel_in_range(hsv_pixel, self.color_ranges['water']):
            return 6  # water

        if self._pixel_in_range(hsv_pixel, self.color_ranges['dry_vegetation']):
            return 1  # dry vegetation

        if self._pixel_in_range(hsv_pixel, self.color_ranges['wet_vegetation']):
            return 2  # wet vegetation

        if self._pixel_in_range(hsv_pixel, self.color_ranges['no_vegetation']):
            return 0  # no vegetation

        return 3  # common vegetation

    def _pixel_in_range(self, pixel, color_range):
        """Check if pixel is within color range"""
        return np.all(pixel >= color_range['lower']) and np.all(pixel <= color_range['upper'])

    def classify_with_clustering(self, image, n_clusters=6):
        """Use K-means clustering to classify terrain types"""
        # Reshape image for clustering
        pixels = image.reshape(-1, 3)

        # Normalize pixel values
        scaler = StandardScaler()
        pixels_normalized = scaler.fit_transform(pixels.astype(np.float32))

        # Apply K-means clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
        labels = kmeans.fit_predict(pixels_normalized)

        # Reshape back to image dimensions
        clustered_image = labels.reshape(image.shape[:2])

        # Map clusters to tile types based on cluster centers
        cluster_centers = scaler.inverse_transform(kmeans.cluster_centers_)
        tile_mapping = self._map_clusters_to_tiles(cluster_centers)

        return clustered_image, tile_mapping

    def _map_clusters_to_tiles(self, cluster_centers):
        mapping = {}

        for i, center in enumerate(cluster_centers):
            bgr_color = np.uint8([[center]])
            hsv_color = cv2.cvtColor(bgr_color, cv2.COLOR_BGR2HSV)[0][0]

            tile_type = self.classify_pixel_by_color(hsv_color)
            mapping[i] = tile_type

        return mapping

    def generate_environmental_properties(self, tile_type):
        """Generate realistic environmental properties for each tile type"""
        np.random.seed()  # Ensure randomness

        if tile_type == 0:  # no vegetation
            humidity = 0.1 + 0.2 * np.random.random()
            fuel = 0.1 + 0.3 * np.random.random()
        elif tile_type == 1:  # dry vegetation
            humidity = 0.1 + 0.3 * np.random.random()
            fuel = 1.5 + 1.0 * np.random.random()
        elif tile_type == 2:  # wet vegetation
            humidity = 0.6 + 0.3 * np.random.random()
            fuel = 0.8 + 0.7 * np.random.random()
        elif tile_type == 3:  # common vegetation
            humidity = 0.3 + 0.4 * np.random.random()
            fuel = 1.0 + 1.0 * np.random.random()
        elif tile_type == 6:  # water
            humidity = 1.0
            fuel = 0.0
        else:
            # Default values
            humidity = 0.5
            fuel = 1.0

        wind_velocity = 0.5 + 2.5 * np.random.random()
        wind_direction = np.random.randint(0, 8)

        return {
            'humidity': round(humidity, 3),
            'fuel': round(fuel, 3),
            'windVelocity': round(wind_velocity, 3),
            'windDirection': wind_direction
        }

    def convert_image_to_map(self, image_path, map_width, map_height, method='color'):
        """Main conversion function"""
        print(f"Converting image {image_path} to {map_width}x{map_height} map...")

        bgr_image, hsv_image = self.preprocess_image(image_path, map_width, map_height)

        if method == 'color':
            tile_map = np.zeros((map_height, map_width), dtype=int)
            for y in range(map_height):
                for x in range(map_width):
                    tile_map[y, x] = self.classify_pixel_by_color(hsv_image[y, x])

        elif method == 'clustering':
            clustered_image, tile_mapping = self.classify_with_clustering(bgr_image)
            tile_map = np.zeros_like(clustered_image)
            for cluster_id, tile_type in tile_mapping.items():
                tile_map[clustered_image == cluster_id] = tile_type

        else:
            raise ValueError("Method must be 'color' or 'clustering'")

        map_data = []
        for y in range(map_height):
            row = []
            for x in range(map_width):
                tile_type = tile_map[y, x]
                properties = self.generate_environmental_properties(tile_type)

                tile_data = {
                    'x': x,
                    'y': y,
                    'type': int(tile_type),
                    'humidity': properties['humidity'],
                    'fuel': properties['fuel'],
                    'windVelocity': properties['windVelocity'],
                    'windDirection': properties['windDirection']
                }
                row.append(tile_data)
            map_data.append(row)

        return map_data, tile_map

    def save_map_data(self, map_data, output_path):
        """Save map data to JSON file"""
        with open(output_path, 'w') as f:
            json.dump(map_data, f, indent=2)
        print(f"Map data saved to {output_path}")

    def visualize_map(self, tile_map, save_path=None):
        colors = {
            0: [200, 200, 200],  # no vegetation - gray
            1: [139, 69, 19],    # dry vegetation - brown
            2: [34, 139, 34],    # wet vegetation - green
            3: [154, 205, 50],   # common vegetation - yellow-green
            6: [0, 191, 255]     # water - blue
        }

        height, width = tile_map.shape
        colored_map = np.zeros((height, width, 3), dtype=np.uint8)

        for tile_type, color in colors.items():
            mask = tile_map == tile_type
            colored_map[mask] = color

        plt.figure(figsize=(12, 8))
        plt.imshow(colored_map)
        plt.title('Generated Tile Map')

        legend_elements = []
        for tile_type, color in colors.items():
            type_name = self.tile_types.get(tile_type, f"Type {tile_type}")
            legend_elements.append(plt.Rectangle((0,0),1,1, facecolor=np.array(color)/255, label=type_name))

        plt.legend(handles=legend_elements, loc='center left', bbox_to_anchor=(1, 0.5))
        plt.axis('off')
        plt.tight_layout()

        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"Visualization saved to {save_path}")

        plt.show()

    def generate_java_compatible_output(self, map_data, java_output_path):
        """Generate Java code to load the map data"""
        java_code = f"""// Generated map data loader
import java.util.Random;

public class GeneratedMapLoader {{

    public static void loadMapFromGenerated(Map mapInstance) {{
        // Clear existing map
        mapInstance.stopAllFireAgents();

        int width = {len(map_data[0])};
        int height = {len(map_data)};

        Random random = new Random();

        // Load generated tile data
"""

        for y, row in enumerate(map_data):
            for x, tile in enumerate(row):
                java_code += f"""        mapInstance.map[{x}][{y}] = new Tile({x}, {y}, {tile['type']},
                {tile['humidity']}, {tile['fuel']}, {tile['windVelocity']},
                Direction.values()[{tile['windDirection']}]);
"""

        java_code += """    }
}"""

        with open(java_output_path, 'w') as f:
            f.write(java_code)

        print(f"Java loader code saved to {java_output_path}")

def main():
    parser = argparse.ArgumentParser(description='Convert image to fire simulation map')
    parser.add_argument('image_path', help='Path to input image')
    parser.add_argument('--width', type=int, default=50, help='Map width')
    parser.add_argument('--height', type=int, default=50, help='Map height')
    parser.add_argument('--method', choices=['color', 'clustering'], default='color',
                       help='Classification method')
    parser.add_argument('--output', default='generated_map.json', help='Output JSON file')
    parser.add_argument('--java-output', default='GeneratedMapLoader.java',
                       help='Output Java file')
    parser.add_argument('--visualize', action='store_true', help='Show visualization')

    args = parser.parse_args()

    converter = ImageToMapConverter()

    try:
        map_data, tile_map = converter.convert_image_to_map(
            args.image_path, args.width, args.height, args.method
        )

        converter.save_map_data(map_data, args.output)
        converter.generate_java_compatible_output(map_data, args.java_output)

        if args.visualize:
            converter.visualize_map(tile_map, 'map_visualization.png')

        print("\nConversion completed successfully!")
        print(f"Map dimensions: {args.width}x{args.height}")
        print(f"Total tiles: {args.width * args.height}")

        unique, counts = np.unique(tile_map, return_counts=True)
        print("\nTile distribution:")
        for tile_type, count in zip(unique, counts):
            type_name = converter.tile_types.get(tile_type, f"Type {tile_type}")
            percentage = (count / tile_map.size) * 100
            print(f"  {type_name}: {count} tiles ({percentage:.1f}%)")

    except Exception as e:
        print(f"Error: {e}")
        return 1

    return 0

if __name__ == "__main__":
    exit(main())