package com.example.shop.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "IMAGES")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "originalFileName")
    private String originalFileName;

    @Column(name = "size")
    private Long size;

    @Column(name = "contentType")
    private String contentType;

    @Column(name = "isPreviewImage")
    private boolean isPreviewImage; // true for the main photo of the listing;


    @Column(name = "bytes", columnDefinition = "LONGBLOB")
    private byte[] bytes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return isPreviewImage == image.isPreviewImage && Objects.equals(id, image.id) && Objects.equals(name, image.name) && Objects.equals(originalFileName, image.originalFileName) && Objects.equals(size, image.size) && Objects.equals(contentType, image.contentType) && Objects.deepEquals(bytes, image.bytes);
    }

}



