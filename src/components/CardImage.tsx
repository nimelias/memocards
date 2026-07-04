import { Image } from 'expo-image';
import { StyleSheet, View } from 'react-native';

type Props = {
  uri: string;
  maxHeight?: number;
};

export function CardImage({ uri, maxHeight = 200 }: Props) {
  return (
    <View style={styles.wrap}>
      <Image
        source={{ uri }}
        style={[styles.image, { maxHeight }]}
        contentFit="contain"
        transition={150}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 8,
    borderRadius: 8,
    overflow: 'hidden',
    backgroundColor: '#f8fafc',
  },
  image: {
    width: '100%',
    minHeight: 80,
  },
});
